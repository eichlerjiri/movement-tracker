package eichlerjiri.movementtracker.utils;

import java.util.ArrayList;

public class StringUtils {

    public static ArrayList<String> splitNonEmpty(String delimiter, String value) {
        ArrayList<String> ret = new ArrayList<>();

        int startIndex = 0;
        while (true) {
            int index = value.indexOf(delimiter, startIndex);
            if (index == -1) {
                addIfNonEmpty(ret, value.substring(startIndex));
                break;
            }

            addIfNonEmpty(ret, value.substring(startIndex, index));
            startIndex = index + delimiter.length();
        }

        return ret;
    }

    private static void addIfNonEmpty(ArrayList<String> ret, String value) {
        value = value.trim();
        if (!value.isEmpty()) {
            ret.add(value);
        }
    }
}
