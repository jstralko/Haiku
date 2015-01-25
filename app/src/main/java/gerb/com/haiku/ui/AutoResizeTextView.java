package gerb.com.haiku.ui;

import android.content.Context;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * Created by gstralko on 4/23/14.
 */
public class AutoResizeTextView extends TextView{

    private float DEFAULT_TEXT_SIZE;
    private static float mMinTextSize = 20.0f;

    public AutoResizeTextView(Context context, AttributeSet attrs,
                                    int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public AutoResizeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AutoResizeTextView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setSingleLine();
        setEllipsize(TextUtils.TruncateAt.END);
        DEFAULT_TEXT_SIZE = getTextSize();
    }

    /*
     * You will need to call this method is your view can be recycle.
     * (i.e. if you bind it to an adapter, the adapter will reuse the
     * view, so you must reset to the default text size, since it
     * could have been auto resized for the previous person.
     */
    public void setDefaultTextSize() {
        setTextSize(pixelsToSp(this.getContext(), DEFAULT_TEXT_SIZE));
    }

    private static float pixelsToSp(Context context, float px) {
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        return px / scaledDensity;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final Layout layout = getLayout();
        if (layout != null) {
            final int lineCount = layout.getLineCount();
            if (lineCount > 0) {
                final int ellipsisCount = layout.getEllipsisCount(lineCount - 1);
                if (ellipsisCount > 0) {
                    final float textSize = getTextSize();
                    if (textSize - 1 >= mMinTextSize) {
                        // textSize is already expressed in pixels
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, (textSize - 1));
                        onMeasure(widthMeasureSpec, heightMeasureSpec);
                    }
                }
            }
        }
    }
}
