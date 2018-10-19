Feature: Updates of dependencies

  Background:

    Given empty repository

    And installable package lib in version 1.0
    And installable package lib in version 2.0

    And installable package app1 in version 1.0
    And dependency to lib version 1.0

    And installable package app2 in version 1.0
    And dependency to lib version 1.0

    And installable package app1 in version 1.5
    And dependency to lib version >= 1.5

    And installable package app2 in version 1.5
    And dependency to lib version >= 1.5


  Scenario: Install two packages with the same dependency
    Given installed package lib in version 1.0
    And installed package app1 in version 1.0
    And installed package app2 in version 1.0

    When start new operation
    And install package app1 version 1.5
    And install package app2 version 1.5
    And resolve operation

    Then changes contains 3 packages
    And changes contains update of app1 from 1.0 to 1.5
    And changes contains update of app2 from 1.0 to 1.5
    And changes contains update of lib from 1.0 to 2.0
    And installation is empty
    And conflicts is empty
    And suggested is empty
    And unresolved is empty
