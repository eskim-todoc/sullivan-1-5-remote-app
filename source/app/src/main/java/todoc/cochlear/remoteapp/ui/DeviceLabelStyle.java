package todoc.cochlear.remoteapp.ui;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps the address suffix visibly secondary without changing the device/session identity.
 */
public final class DeviceLabelStyle
{
    private static final int ADDRESS_COLOR = 0xffaebec7;
    private static final float ADDRESS_SCALE = 0.82f;
    private static final Pattern TRAILING_ADDRESS = Pattern.compile(
        "(?:\\s*·\\s*)?(?:MAC\\s+)?((?:[0-9a-fA-F]{2}:){1,5}[0-9a-fA-F]{2})$");
    private static final Pattern SPOKEN_ADDRESS = Pattern.compile(
        "(?:\\s*·\\s*|\\s+MAC\\s+|^MAC\\s+|^)((?:[0-9a-fA-F]{2}:){1,5}[0-9a-fA-F]{2})(?=$|[.,])");

    private DeviceLabelStyle()
    {
    }

    public static CharSequence format(String label)
    {
        String value = label == null ? "" : label;
        Matcher address = TRAILING_ADDRESS.matcher(value);
        if (!address.find())
        {
            return value;
        }
        String identity = value.substring(0, address.start()).trim();
        String prefix = identity.isEmpty() ? "" : identity + "  ";
        SpannableString text = new SpannableString(prefix + "MAC "
            + address.group(1).toUpperCase(Locale.ROOT));
        text.setSpan(new ForegroundColorSpan(ADDRESS_COLOR), prefix.length(), text.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new RelativeSizeSpan(ADDRESS_SCALE), prefix.length(), text.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }

    public static String forAccessibility(String label)
    {
        String value = label == null ? "" : label;
        Matcher address = SPOKEN_ADDRESS.matcher(value);
        StringBuffer description = new StringBuffer();
        while (address.find())
        {
            String prefix = address.start() == 0 ? "" : ", ";
            address.appendReplacement(description, Matcher.quoteReplacement(prefix
                + "블루투스 주소 끝 " + address.group(1).toUpperCase(Locale.ROOT)));
        }
        address.appendTail(description);
        return description.toString();
    }
}
