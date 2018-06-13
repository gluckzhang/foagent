package se.kth.chaos;

import java.util.regex.Pattern;

public class FilterByClassAndMethodName {

    private final Pattern pattern;

    public FilterByClassAndMethodName(String regex) {
        this.pattern = Pattern.compile(regex.replace("$", "\\$"));
    }

    public boolean matches(String className, String methodName) {
        String fullName = className + "/" + methodName;

        return this.pattern.matcher(fullName).find();
    }
}
