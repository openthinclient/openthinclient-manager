# Created by joerg at 05.04.16

Feature: Package Manager Operation Computation for defined Testcases
  these scenarios are taken form https://wiki.openthinclient.org/confluence/pages/viewpage.action?pageId=1966738
  Frage: isSamePackage für Installation notwendig?

  Background:
    Given installable package foo in version 2.0-1
    And conflicts to foo2
    And provides foo2
    
    And installable package foo in version 2.1-1
    And conflicts to foo2
    
    And installable package foo-fork in version 2.0-1
    And conflicts to zonk
    And provides foo
    
    And installable package foo2 in version 2.0-1
    And conflicts to foo
    
    And installable package zonk in version 2.0-1
    And conflicts to bar2
    
    And installable package zonk in version 2.1-1
    And dependency to foo
    And conflicts to bar2

    And installable package zonk-dev in version 2.0-1
    And conflicts to foo 
    And replaces foo
    
    And installable package zonk2 in version 2.0-1
    And dependency to bas2

    And installable package bar in version 2.0-1
    And dependency to foo version 2.1-1
    And conflicts to zonk version 2.0-1

    And installable package bar2 in version 2.0-1
    And dependency to foo
    
  Scenario: Installation eines einzelnen Pakets
    When start new operation
    And install package foo version 2.0-1
    And resolve operation
    Then installation contains foo version 2.0-1
    And dependencies is empty
    And suggested is empty
    And conflicts is empty
    
  Scenario: Installation von zwei Paketen - keine Abhängigkeit zueinander
    When start new operation
    And install package foo version 2.0-1
    And install package zonk version 2.0-1
    And resolve operation
    Then installation contains foo version 2.0-1
    And installation contains zonk version 2.0-1
    And dependencies is empty
    And suggested is empty
    And conflicts is empty
    
  Scenario: Installation eines einzelnen Pakets das ein Paket benötigt welches vorhanden ist
    Given installed package foo in version 2.0-1
    When start new operation
    And install package bar2 version 2.0-1
    And resolve operation    
    Then installation contains bar2 version 2.0-1
    And dependencies is empty
    And suggested is empty
    And conflicts is empty
    
  Scenario: Installation eines einzelnen Pakets das ein Paket benötigt welches nicht vorhanden ist
    When start new operation
    And install package foo version 2.0-1
    And install package bar2 version 2.0-1
    And resolve operation    
    Then installation contains foo version 2.0-1
    And installation contains bar version 2.0-1
    And dependencies is empty
    And suggested is empty
    And conflicts is empty
    