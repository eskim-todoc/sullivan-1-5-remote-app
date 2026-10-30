package todoc.cochlear.remoteapp.common;

/** java.util.function을 제공하지 않는 API 23에서도 사용 가능한 조건 콜백. */
@FunctionalInterface
public interface BooleanCondition
{
    boolean getAsBoolean();
}
