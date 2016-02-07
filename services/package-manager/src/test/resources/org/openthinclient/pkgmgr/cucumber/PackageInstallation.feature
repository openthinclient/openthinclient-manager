# Created by francois at 02.02.16
Feature: Package Install
  # Enter feature description here

  Scenario: Install single package
    Given empty manager home
    When install package foo
    Then manager home contains file schema/application/foo.xml
    And manager home contains file schema/application/foo-tiny.xml.sample
    And manager home contains file sfs/package/foo.sfs

  Scenario: Install two packages
    Given empty manager home
    When install package foo
    And install package bar
    Then manager home contains file schema/application/foo.xml
    And manager home contains file schema/application/foo-tiny.xml.sample
    And manager home contains file sfs/package/foo.sfs
    And manager home contains file schema/application/bar.xml
    And manager home contains file schema/application/bar-tiny.xml.sample
    And manager home contains file sfs/package/bar.sfs
