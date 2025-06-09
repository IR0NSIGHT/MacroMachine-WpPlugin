package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

public interface IDisplayUnit {
    String getName();

    String getDescription();

    String getToolTipText();
    static boolean matchesFilterString(String s, IDisplayUnit item) {
        String lowerCaseString = s.toLowerCase();
        return item.getName().toLowerCase().contains(lowerCaseString) ||
                item.getDescription().toLowerCase().contains(lowerCaseString) ||
                lowerCaseString.contains(item.getName().toLowerCase()) ||
                lowerCaseString.contains(item.getDescription().toLowerCase());
    };
}
