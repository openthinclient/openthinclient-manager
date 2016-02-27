# Created by francois at 22.02.16
Feature: Package Manager Operation Computation
  these scenarios describe and test common interactions with the PackageManagerOperation

  Background:
    Given package no-deps in version 3.2-1
    And package single-dep in version 1.2-2
    And dependency to no-deps version 3.2-1
    And package versioned in version 1.0-1
    And package versioned in version 1.2-1

  Scenario: Install package with no dependencies
    When start new operation
    And install package no-deps version 3.2-1
    And resolve operation

    Then dependencies is empty
    And suggested is empty

  Scenario: Install package with a single dependency
    When start new operation
    And install package single-dep version 1.2-2
    And resolve operation

    Then dependencies contains no-deps version 3.2-1
    And suggested is empty

  Scenario: Update already installed package
    When installation contains versioned version 1.0-1
    And start new operation
    And install package versioned version 1.2-1
    And resolve operation

    Then dependencies is empty
    And changes contains update of versioned from 1.0-1 to 1.2-1

  Scenario: Uninstall already installed package
    When installation contains versioned version 1.0-1
    And start new operation
    And uninstall package versioned version 1.0-1
    And resolve operation

    Then dependencies is empty
    And changes is empty
    And suggested is empty
    And uninstalling contains versioned version 1.0-1