package autograder.autotester;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

final class Literals {
    private Literals() { }

    static String source(Object value) {
        return source(value, null);
    }

    static String source(Object value, String targetType) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Long number) return number + "L";
        if (value instanceof Double number) {
            if (number.isNaN()) return "Double.NaN";
            if (number == Double.POSITIVE_INFINITY) return "Double.POSITIVE_INFINITY";
            if (number == Double.NEGATIVE_INFINITY) return "Double.NEGATIVE_INFINITY";
            return number.toString();
        }
        if (value instanceof Integer || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof ShapeInput shape) {
            if (shape.kind().equals("circle")) {
                return "new Circle(" + quote(shape.id()) + ", " + shape.firstDimension() + ")";
            }
            return "new Rectangle(" + quote(shape.id()) + ", " + shape.firstDimension()
                    + ", " + shape.secondDimension() + ")";
        }
        if (value instanceof Collection<?> values) {
            String elementType = "Object";
            if (targetType != null && targetType.contains("<") && targetType.endsWith(">")) {
                elementType = targetType.substring(targetType.indexOf('<') + 1, targetType.length() - 1);
            }
            return "new ArrayList<" + elementType + ">(java.util.Arrays.asList(" + values.stream()
                    .map(Literals::source).collect(java.util.stream.Collectors.joining(", ")) + "))";
        }
        if (value instanceof Object[] values) {
            return "new Object[] {" + Arrays.stream(values).map(Literals::source)
                    .collect(java.util.stream.Collectors.joining(", ")) + "}";
        }
        if (value instanceof int[] values) {
            return "new int[] {" + join(values) + "}";
        }
        if (value instanceof String[] values) {
            return "new String[] {" + Arrays.stream(values).map(Literals::quote)
                    .collect(java.util.stream.Collectors.joining(", ")) + "}";
        }
        throw new IllegalArgumentException("value type cannot be written as a Java literal: "
                + value.getClass().getTypeName());
    }

    static String show(Object value) {
        if (value == null) return "null";
        if (value instanceof int[] values) return Arrays.toString(values);
        if (value instanceof String[] values) return Arrays.toString(values);
        if (value instanceof Collection<?> values) {
            return values.stream().map(Literals::show)
                    .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
        }
        if (value instanceof String text) return quote(text);
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            StringBuilder display = new StringBuilder("[");
            for (int index = 0; index < length; index++) {
                if (index > 0) display.append(", ");
                display.append(Array.get(value, index));
            }
            return display.append(']').toString();
        }
        return value.toString();
    }

    static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                default -> {
                    if (Character.isISOControl(character)) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    private static String join(int[] values) {
        return Arrays.stream(values).mapToObj(Integer::toString)
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
