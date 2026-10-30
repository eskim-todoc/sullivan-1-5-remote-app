package todoc.cochlear.remoteapp.ff;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 한 연결의 FF3 트랜잭션. 호출·응답·타이머는 모두 메인 스레드에서 실행한다.
 */
public final class Ff3Session
{
    // java.util.function.Consumer는 API 24부터다. minSdk 23에서도 같은 경로를 사용한다.
    private interface PayloadCallback
    {
        void accept(byte[] payload);
    }

    public interface Transport
    {
        boolean ready();

        void send(byte[] packet);

        void later(Runnable action, long delayMs);

        void changed();
    }

    public enum State
    {IDLE, READING, EDITING, SAVING, CHANGING_MODE, DIAGNOSTICS, ERROR}

    private final Transport io;
    private final ArrayDeque<Runnable> queue = new ArrayDeque<>();
    private PayloadCallback pending;
    private int access, index, expectedLength, expectedValue, ticket, epoch;
    private boolean busy, desynchronized;
    private Runnable completion;
    private Ff3Config config = new Ff3Config();
    private final Map<Integer, byte[]> diagnostics = new LinkedHashMap<>();
    public State state = State.IDLE;
    public int schema = -1, mode = -1, status = -1;
    public byte[] file;
    public String message = "설정을 조회하세요.";

    public Ff3Session(Transport transport)
    {
        io = transport;
    }

    public boolean busy()
    {
        return busy;
    }

    public boolean awaiting()
    {
        return pending != null;
    }

    public boolean supported()
    {
        return schema == 3;
    }

    public boolean settingsReady()
    {
        return supported() && config.complete() && !desynchronized && state != State.ERROR;
    }

    public boolean editing()
    {
        return status >= 0 && (status & 4) != 0;
    }

    public boolean needsReconnect()
    {
        return desynchronized;
    }

    public Ff3Config config()
    {
        return config.copy();
    }

    public Map<Integer, byte[]> diagnostics()
    {
        return new LinkedHashMap<>(diagnostics);
    }

    public void reset()
    {
        epoch++;
        ticket++;
        pending = null;
        busy = false;
        desynchronized = false;
        queue.clear();
        completion = null;
        schema = mode = status = -1;
        file = null;
        config = new Ff3Config();
        diagnostics.clear();
        state = State.IDLE;
        message = "연결 후 설정을 조회하세요.";
        io.changed();
    }

    private boolean start(State next, String text)
    {
        if (busy)
        {
            return false;
        }
        if (desynchronized)
        {
            message = "응답 순서를 확인할 수 없습니다. 재연결 후 조회하세요.";
            io.changed();
            return false;
        }
        if (!io.ready())
        {
            message = "다른 통신이 끝난 뒤 다시 시도하세요.";
            io.changed();
            return false;
        }
        epoch++;
        queue.clear();
        completion = null;
        busy = true;
        state = next;
        message = text;
        io.changed();
        return true;
    }

    private void finish(String text)
    {
        busy = false;
        state = editing() ? State.EDITING : State.IDLE;
        completion = null;
        message = text;
        io.changed();
    }

    private void fail(String text, boolean unknown)
    {
        ticket++;
        queue.clear();
        pending = null;
        completion = null;
        busy = false;
        desynchronized |= unknown;
        state = State.ERROR;
        message = text + (unknown ? " 반영 상태 불명: 재연결 후 재조회하세요." : " 상태를 재조회한 뒤 계속하거나 편집을 폐기하세요.");
        config = new Ff3Config();
        status = -1;
        mode = -1;
        file = null;
        diagnostics.clear();
        io.changed();
    }

    private void next()
    {
        if (!busy || pending != null)
        {
            return;
        }
        if (!queue.isEmpty())
        {
            queue.remove().run();
        }
        else if (completion != null)
        {
            Runnable done = completion;
            completion = null;
            done.run();
        }
    }

    private void request(int a, int i, int value, int len, PayloadCallback callback)
    {
        access = a;
        index = i;
        expectedValue = value;
        expectedLength = len;
        pending = callback;
        int current = ++ticket;
        io.later(() ->
        {
            if (pending != null && ticket == current)
            {
                fail("응답 시간 초과.", true);
            }
        }, 3000);
        io.send(a == 0 ? new byte[]{0x59, 0x20, 0, (byte) i} : new byte[]{0x59, 0x20, 1, (byte) i, (byte) value});
    }

    private void read(int i, int len, PayloadCallback cb)
    {
        queue.add(() -> request(0, i, -1, len, cb));
    }

    private void write(int i, int value, Runnable cb)
    {
        queue.add(() -> request(1, i, value, 1, p ->
        {
            if (cb != null)
            {
                cb.run();
            }
        }));
    }

    /**
     * 20B 패딩은 무시하되 access/index/L/쓰기 에코를 모두 검증한다.
     */
    public void receive(byte[] packet)
    {
        if (pending == null)
        {
            return;
        }
        if (packet.length >= 2 && (packet[0] & 255) == 0xF0 && (packet[1] & 255) == 0x59)
        {
            if (packet.length < 6)
            {
                fail("잘린 오류 응답.", true);
                return;
            }
            int major = packet[2] & 255, minor = packet[3] & 255;
            String reason = major == 2 && minor == 1 ? "파일 저장·처리 실패" : "범위·형식·현재 상태 또는 활성 조건 거부";
            fail(reason + " (" + major + "/" + minor + ", line " + (((packet[4] & 255) << 8) | (packet[5] & 255)) + ").", false);
            return;
        }
        if (packet.length < 5 || packet.length > 20 || (packet[0] & 255) != 0x59 || (packet[1] & 255) != 0x20
            || (packet[2] & 255) != access || (packet[3] & 255) != index || (packet[4] & 255) != expectedLength
            || packet.length < 5 + expectedLength || (access == 1 && (packet[5] & 255) != expectedValue))
        {
            fail("요청과 응답이 일치하지 않습니다.", true);
            return;
        }
        byte[] payload = Arrays.copyOfRange(packet, 5, 5 + expectedLength);
        PayloadCallback cb = pending;
        pending = null;
        ticket++;
        cb.accept(payload);
        next();
    }

    private void readMode()
    {
        read(17, 1, p ->
        {
            mode = p[0] & 255;
            if (mode != 0 && mode != 1 && mode != 3)
            {
                fail("알 수 없는 FF 모드.", false);
            }
        });
    }

    private void readStatus()
    {
        read(39, 1, p -> status = p[0] & 255);
    }

    private void readFile()
    {
        read(116, 10, p -> file = p);
    }

    private void readSettings(Ff3Config target)
    {
        for (int i : Ff3Config.indexes())
        {
            read(i, 1, p -> target.set(i, p[0] & 255));
        }
    }

    /**
     * 연결 시 요약만 읽는다. 세부 설정은 사용자가 확인할 때까지 미조회로 둔다.
     */
    public void loadSummary()
    {
        if (!start(State.READING, "FF 상태 확인 중…"))
        {
            return;
        }
        read(38, 1, p ->
        {
            schema = p[0] & 255;
            if (schema != 3)
            {
                finish("지원하지 않는 FF3 schema " + schema);
                return;
            }
            readMode();
            readStatus();
            completion = () -> finish("FF 설정 확인을 눌러 제어를 준비하세요.");
        });
        next();
    }

    public void load()
    {
        if (!start(State.READING, "FF3 설정 조회 중…"))
        {
            return;
        }
        config = new Ff3Config();
        file = null;
        status = mode = schema = -1;
        read(38, 1, p ->
        {
            schema = p[0] & 255;
            if (schema != 3)
            {
                finish("지원하지 않는 FF3 schema " + schema + ". 쓰기를 차단했습니다.");
                return;
            }
            readMode();
            readStatus();
            readFile();
            readSettings(config);
            completion = () -> finish(editing() ? "저장하지 않은 장치 편집이 남아 있습니다. 이어서 바꾸거나 변경 취소를 선택하세요." : "장치에서 설정을 읽었습니다. 지금은 확인만 가능합니다.");
        });
        next();
    }

    public void begin()
    {
        if (!supported() || !config.complete() || status < 0 || !start(State.READING, "보상을 끄고 설정 준비 중…"))
        {
            return;
        }
        if (editing())
        {
            finish("이전 편집값을 계속 편집합니다.");
            return;
        }
        write(17, 0, null);
        readMode();
        completion = () ->
        {
            if (mode != 0)
            {
                fail("OFF 재조회 불일치.", false);
                return;
            }
            write(37, 1, null);
            readStatus();
            config = new Ff3Config();
            readSettings(config);
            completion = () ->
            {
                if (!editing())
                {
                    fail("편집 시작이 확인되지 않았습니다.", false);
                }
                else
                {
                    finish("설정하는 중입니다. 변경한 값은 저장 전까지 동작에 사용되지 않습니다.");
                }
            };
            next();
        };
        next();
    }

    public void abort()
    {
        if (!supported() || !editing() || mode != 0 || !start(State.READING, "변경을 취소하고 저장된 값 읽는 중…"))
        {
            return;
        }
        write(37, 4, null);
        readStatus();
        config = new Ff3Config();
        readSettings(config);
        completion = () ->
        {
            if (editing())
            {
                fail("편집 종료가 확인되지 않았습니다.", false);
            }
            else
            {
                finish("변경을 취소했습니다. 보상은 꺼져 있습니다.");
            }
        };
        next();
    }

    public void save(Ff3Config desired)
    {
        String error = desired.validate();
        if (error != null)
        {
            message = error;
            io.changed();
            return;
        }
        if (!supported() || mode != 0 || !editing() || !config.complete() || !start(State.SAVING, "편집값 전송·검증·저장 중…"))
        {
            return;
        }
        Ff3Config expected = desired.copy();
        for (int i : Ff3Config.indexes())
        {
            if (expected.get(i) != config.get(i))
            {
                write(i, expected.get(i), null);
            }
        }
        write(37, 2, null);
        write(37, 3, null);
        Ff3Config actual = new Ff3Config();
        readMode();
        readSettings(actual);
        readStatus();
        readFile();
        completion = () ->
        {
            config = actual;
            if (mode != 0 || editing() || !actual.sameValues(expected) || !fileConfirmed())
            {
                fail("저장 후 설정·편집 상태·파일 확인이 일치하지 않습니다.", false);
                return;
            }
            awaitAck(20, () -> finish("설정 저장됨 · 보상은 꺼져 있습니다. 아래에서 이 설정으로 켤 수 있습니다."));
        };
        next();
    }

    private boolean fileConfirmed()
    {
        return file != null && (file[0] & 255) == 3 && file[9] == 0;
    }

    private void awaitAck(int remaining, Runnable done)
    {
        if ((status & 8) != 0)
        {
            done.run();
            return;
        }
        if (remaining == 0)
        {
            fail("입력 처리부의 설정 확인이 늦어지고 있습니다. 보상은 자동으로 켜지지 않습니다.", false);
            return;
        }
        int connection = epoch;
        io.later(() ->
        {
            if (!busy || epoch != connection)
            {
                return;
            }
            readStatus();
            completion = () -> awaitAck(remaining - 1, done);
            next();
        }, 100);
    }

    public void changeMode(int desired)
    {
        if (!supported() || !config.complete() || editing() || status < 0 || (desired != 0 && desired != 1 && desired != 3))
        {
            return;
        }
        if (desired == 3 && config.validate() != null)
        {
            message = config.validate();
            io.changed();
            return;
        }
        if (!start(State.CHANGING_MODE, "FF 모드 저장·재조회 중…"))
        {
            return;
        }
        readStatus();
        readFile();
        completion = () ->
        {
            if (editing())
            {
                fail("설정 편집 중입니다. 저장하거나 편집을 취소한 후 모드를 선택하세요.", false);
                return;
            }
            if (desired == 3)
            {
                if ((status & 1) == 0)
                {
                    fail("FF3 실행 설정이 유효하지 않습니다. 설정을 확인하고 저장하세요.", false);
                    return;
                }
                if ((status & 8) == 0)
                {
                    fail("입력 처리부가 설정을 아직 반영하지 않았습니다. 잠시 후 FF 설정 확인을 다시 누르세요.", false);
                    return;
                }
                if ((status & 16) == 0)
                {
                    fail("FF3 전압 보정 기록을 사용할 수 없습니다. FF를 OFF로 저장한 뒤 외부기 전원을 껐다 켜고 FF 설정을 다시 확인하세요. BLE 재연결만으로는 복구되지 않습니다.", false);
                    return;
                }
            }
            // The firmware saves and verifies AUDIOFF.TXT as part of the mode write.
            // An unconfirmed file before that write must not block first activation.
            write(17, desired, null);
            readMode();
            readFile();
            completion = () ->
            {
                if (mode != desired)
                {
                    fail("요청한 FF 모드가 적용되지 않았습니다. FF 설정을 다시 확인하세요.", false);
                }
                else if (!fileConfirmed())
                {
                    fail("FF 모드는 변경됐지만 설정 파일 저장을 확인하지 못했습니다. FF 설정을 다시 확인하세요.", false);
                }
                else
                {
                    finish(Ff3Config.modeName(mode) + " 선택됨 · 실제 실행 조건은 별도입니다.");
                }
            };
            next();
        };
        next();
    }

    public void readDiagnostics()
    {
        if (!settingsReady() || !start(State.DIAGNOSTICS, "최근 동작을 장치에서 읽는 중…"))
        {
            return;
        }
        diagnostics.clear();
        for (int i = 112; i <= 118; i++)
        {
            final int key = i;
            read(i, i == 116 ? 10 : 15, p -> diagnostics.put(key, p));
        }
        completion = () -> finish("최근 동작을 읽었습니다. 이 화면은 자동으로 갱신되지 않습니다.");
        next();
    }

    public static long unsigned(byte[] p, int start, int length)
    {
        long value = 0;
        for (int i = 0; i < length; i++)
        {
            value = (value << 8) | (p[start + i] & 255);
        }
        return value;
    }
}
