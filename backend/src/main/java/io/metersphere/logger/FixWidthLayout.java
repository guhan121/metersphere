package io.metersphere.logger;


import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;

import java.text.SimpleDateFormat;

/**
 * @author qiantao
 */
public class FixWidthLayout extends LayoutBase<ILoggingEvent> {
    final static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
    final static int MAX_CLASS_NAME_LENGTH = 40;
    final static int MAX_LEVEL_LENGTH = 5;

    public static int getStringWidth(String str){
        char[] c = str.toCharArray();
        int width = 0;
        for (int i = c.length - 1; i >= 0; i--) {
            if (c[i] > 256) {
                width += 2;
            } else {
                width += 1;
            }
        }
        return width;
    }
    public static String getRightSubStringByWidth(String str, int width) {
        assert width >= 2;
        char[] c = str.toCharArray();
        int right = 0;
        for (int i = c.length - 1; i > 0; i--) {
            if (c[i] > 256) {
                right += 2;
            } else {
                right += 1;
            }
            if (right == width) {
                return str.substring(i);
            } else if (right == width - 1) {
                if (c[i - 1] > 256) {
                    return str.substring(i);
                } else {
                    return str.substring(i - 1);
                }
            }
        }
        return str;
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        StackTraceElement[] stackTraceElements = event.getCallerData();
        String className = "";
//        String simpleClassName = "";
        int lineNumber = 0;
//        boolean isNativeMethod = false;
        if (stackTraceElements != null && stackTraceElements.length > 0) {
            StackTraceElement first = stackTraceElements[0];
            className = first.getClassName();
//            simpleClassName = (className.indexOf('.') >= 0) ? className.substring(className.lastIndexOf('.') + 1) : className;
            lineNumber = first.getLineNumber();
//            isNativeMethod = first.isNativeMethod();
        }

        StringBuilder sbuf = new StringBuilder(1024);
        sbuf.append(SDF.format(event.getTimeStamp()));
        int eventLevel = MAX_LEVEL_LENGTH - event.getLevel().levelStr.length();
        sbuf.append(" ");
        for (int i = 0; i < eventLevel; i++) {
            sbuf.append(" ");
        }
        sbuf.append(event.getLevel());
        sbuf.append(" ");
        String string = getRightSubStringByWidth(event.getLoggerName(), MAX_CLASS_NAME_LENGTH);
        int left = MAX_CLASS_NAME_LENGTH - getStringWidth(string);
        for (int i = 0; i < left; i++) {
            sbuf.append(" ");
        }
        sbuf.append(string);
        sbuf.append(":");
        sbuf.append(String.format("%4d", lineNumber));
        sbuf.append(" - ");
        sbuf.append(event.getFormattedMessage());
        sbuf.append(CoreConstants.LINE_SEPARATOR);
        return sbuf.toString();
    }
}