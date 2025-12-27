package com.bingbaihanji.code.resurrector.link;

import com.bingbaihanji.code.resurrector.ui.component.Selection;

import java.util.Map;
import java.util.Set;

public interface LinkProvider {

    void generateContent();

    String getTextContent();

    void processLinks();

    Map<String, Selection> getDefinitionToSelectionMap();

    Map<String, Set<Selection>> getReferenceToSelectionsMap();

    boolean isLinkNavigable(String uniqueStr);

    String getLinkDescription(String uniqueStr);

}
