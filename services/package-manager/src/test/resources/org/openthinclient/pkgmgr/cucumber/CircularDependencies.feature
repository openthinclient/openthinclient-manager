Feature: Circular dependencies

  Background:

    Given empty repository

    And installable package gabel in version 2.1-1
    And dependency to messer

    And installable package gabel in version 2.1-2
    And dependency to messer version >= 2.1-2

    And installable package messer in version 2.1-1
    And dependency to gabel

    And installable package messer in version 2.1-2
    And dependency to gabel version >= 2.1-2

    And installable package besteck in version 2.1-1
    And dependency to gabel

    And installable package besteck in version 2.1-2
    And dependency to gabel version >= 2.1-2

  Scenario: Installation eines Paketes, das zirkulaer abhaengige Pakete benoetigt
    When start new operation
    And install package besteck version 2.1-1
    And resolve operation
    Then installation contains 3 packages
    And installation contains besteck version 2.1-1
    And installation contains messer version 2.1-2
    And installation contains gabel version 2.1-2
    And changes is empty
    And suggested is empty
    And conflicts is empty
    And unresolved is empty

  Scenario: Update eines Paketes, das zirkulaer abhaengige Pakete benoetigt, die auch geupdatet werden muessen
    Given installed package besteck in version 2.1-1
    And installed package gabel in version 2.1-1
    And installed package messer in version 2.1-1
    When start new operation
    And install package besteck version 2.1-2
    And resolve operation
    Then installation is empty
    And changes contains update of besteck from 2.1-1 to 2.1-2
    And changes contains update of gabel from 2.1-1 to 2.1-2
    And changes contains update of messer from 2.1-1 to 2.1-2
    And suggested is empty
    And conflicts is empty
    And unresolved is empty
