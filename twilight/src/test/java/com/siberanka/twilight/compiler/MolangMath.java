package com.siberanka.twilight.compiler;

import java.util.*;

/** Evaluates the numeric Molang subset emitted by the display compiler (angles in degrees).
 * Unknown variables/functions fail instead of silently becoming zero. Not a client emulator. */
final class MolangMath {
    final Map<String, Double> variables = new HashMap<>();

    void run(String script, Map<String, Double> properties) {
        for (var entry : properties.entrySet()) {
            script = script.replace("q.property('" + entry.getKey() + "')", "(" + entry.getValue() + ")");
        }
        for (String statement : script.split(";")) {
            if (statement.isBlank()) continue;
            int equals = statement.indexOf('=');
            variables.put(statement.substring(0, equals).trim(), eval(statement.substring(equals + 1)));
        }
    }

    double eval(String source) {
        Parser parser = new Parser(source);
        double value = parser.conditional();
        if (parser.position != parser.source.length()) throw new IllegalArgumentException(source);
        return value;
    }

    private final class Parser {
        final String source;
        int position;
        Parser(String source) { this.source = source.replace(" ", ""); }
        boolean take(String text) {
            if (!source.startsWith(text, position)) return false;
            position += text.length();
            return true;
        }
        void expect(String text) { if (!take(text)) throw new IllegalArgumentException(source.substring(position)); }
        double conditional() {
            double value = compare();
            if (take("?")) {
                double yes = conditional();
                expect(":");
                double no = conditional();
                return value != 0 ? yes : no;
            }
            return value;
        }
        double compare() {
            double value = add();
            if (take("==")) return value == add() ? 1 : 0;
            if (take(">")) return value > add() ? 1 : 0;
            if (take("<")) return value < add() ? 1 : 0;
            return value;
        }
        double add() {
            double value = multiply();
            while (true) {
                if (take("+")) value += multiply();
                else if (take("-")) value -= multiply();
                else return value;
            }
        }
        double multiply() {
            double value = atom();
            while (true) {
                if (take("*")) value *= atom();
                else if (take("/")) value /= atom();
                else return value;
            }
        }
        double atom() {
            if (take("-")) return -atom();
            if (take("+")) return atom();
            if (take("(")) { double value = conditional(); expect(")"); return value; }
            int start = position;
            if (position < source.length() && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '.')) {
                while (position < source.length() && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '.')) position++;
                if (take("E") || take("e")) {
                    if (!take("-")) take("+");
                    while (position < source.length() && Character.isDigit(source.charAt(position))) position++;
                }
                return Double.parseDouble(source.substring(start, position));
            }
            while (position < source.length() && (Character.isLetterOrDigit(source.charAt(position)) || "._".indexOf(source.charAt(position)) >= 0)) position++;
            String name = source.substring(start, position);
            if (!take("(")) return Objects.requireNonNull(variables.get(name), "Unknown variable " + name);
            List<Double> args = new ArrayList<>();
            do { args.add(conditional()); } while (take(","));
            expect(")");
            double a = args.getFirst();
            return switch (name) {
                case "math.abs" -> Math.abs(a);
                case "math.sqrt" -> Math.sqrt(a);
                case "math.sin" -> Math.sin(Math.toRadians(a));
                case "math.asin" -> Math.toDegrees(Math.asin(a));
                case "math.acos" -> Math.toDegrees(Math.acos(a));
                case "math.atan2" -> Math.toDegrees(Math.atan2(a, args.get(1)));
                case "math.max" -> Math.max(a, args.get(1));
                case "math.clamp" -> Math.clamp(a, args.get(1), args.get(2));
                case "math.mod" -> a % args.get(1);
                default -> throw new IllegalArgumentException("Unknown function " + name);
            };
        }
    }
}
