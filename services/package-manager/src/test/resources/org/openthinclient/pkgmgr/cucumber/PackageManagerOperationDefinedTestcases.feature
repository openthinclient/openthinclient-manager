# Created by joerg at 05.04.16

# fix test failures
# unistall conflicts implementieren
# dependencies implementieren

Feature: Package Manager Operation Computation for defined Testcases
  these scenarios are taken form https://wiki.openthinclient.org/confluence/pages/viewpage.action?pageId=1966738
  Frage: isSamePackage für Installation notwendig?

  Background:

    Given installable package foo in version 2.0-1
    And conflicts to foo2
    And provides foo
    
    And installable package foo in version 2.1-1
    And conflicts to foo2
    
    And installable package foo-fork in version 2.0-1
    And conflicts to zonk
    And provides foo
    
    And installable package foo2 in version 2.0-1
    And conflicts to foo

    And installable package foo3 in version 2.0-1
    And conflicts to foo2
    And provides foo
    And suggests bas
    
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
    And dependency to foo version >= 2.1-1 
    And conflicts to zonk version <= 2.0-1 

    And installable package bar2 in version 2.0-1
    And dependency to foo
    
    And installable package bar2 in version 2.1-1
    And conflicts to foo
    And replaces foo
    
    And installable package bar2-dev in version 2.0-1
    And dependency to bar2 
    And conflicts to foo version <= 2.0-1 

    And installable package bas in version 2.0-1
    And dependency to foo version >= 2.0-1     
    
    And installable package bas in version 2.1-1
    And dependency to foo version >= 2.1-1         
    
    And installable package bas-dev in version 2.0-1
    And dependency to foo
    And conflicts to foo
    And replaces foo
    
    And installable package bas2 in version 2.0-1
    And dependency to zonk2
    
  Scenario: Installation eines einzelnen Pakets
    When start new operation
    And install package foo version 2.0-1
    And resolve operation
    Then installation contains foo version 2.0-1
    # And dependencies is empty
    And suggested is empty
    And conflicts is empty
    
  Scenario: Installation von zwei Paketen - keine Abhängigkeit zueinander
    When start new operation
    And install package foo version 2.0-1
    And install package zonk version 2.0-1
    And resolve operation
    Then installation contains foo version 2.0-1
    And installation contains zonk version 2.0-1
    # And dependencies is empty
    And suggested is empty
    And conflicts is empty
    
  Scenario: Installation eines einzelnen Pakets das ein Paket benötigt welches vorhanden ist
    Given installed package foo in version 2.0-1
    When start new operation
    And install package bar2 version 2.0-1
    And resolve operation    
    Then installation contains bar2 version 2.0-1
    # And dependencies is empty
    And suggested is empty
    And conflicts is empty
    
  Scenario: Installation eines einzelnen Pakets das ein Paket benötigt welches nicht vorhanden ist
    When start new operation
    And install package foo version 2.0-1
    And install package bar2 version 2.0-1
    And resolve operation    
    Then installation contains foo version 2.0-1
    And installation contains bar2 version 2.0-1
    # And dependencies is empty
    And suggested is empty
    And conflicts is empty

  Scenario: Installation eines einzelnen Pakets das ein Paket benötigt welches vorhanden ist, aber zu alt oder zu neu ist
    Given installed package foo in version 2.0-1
    When start new operation
    And install package foo version 2.1-1
    And install package bar2 version 2.0-1
    And resolve operation    
    Then installation contains bar2 version 2.0-1
    And changes contains update of foo from 2.0-1 to 2.1-1
    And suggested is empty
    And conflicts is empty
    
  Scenario: Installation eines einzelnen Pakets welches einen Konflikt mit einem bereits existierenden Pakets hat
    Given installed package foo in version 2.0-1
    And installed package bar2 in version 2.0-1
    When start new operation
    And uninstall package bar2 version 2.0-1
    And install package zonk version 2.0-1
    And resolve operation    
    Then installation contains zonk version 2.0-1
    And uninstalling contains bar2 version 2.0-1
    And changes is empty
    And suggested is empty
    And conflicts is empty      
    
  Scenario: Installation eines einzelnen Pakets welches einen Konflikt mit der Version eines bereits existierenden Pakets hat, Zonk Deinstallieren
    Given installed package foo in version 2.0-1
    And installed package zonk in version 2.0-1
    When start new operation
    And uninstall package zonk version 2.0-1
    And install package bar version 2.0-1
    And resolve operation    
    Then installation contains bar version 2.0-1
    And uninstalling contains zonk version 2.0-1
    And changes is empty
    And suggested is empty
    And conflicts is empty      
    
  Scenario: Installation eines einzelnen Pakets welches einen Konflikt mit der Version eines bereits existierenden Pakets hat, Update Zonk
    Given installed package foo in version 2.0-1
    And installed package zonk in version 2.0-1
    When start new operation
    And install package zonk version 2.1-1
    And install package bar version 2.0-1
    And resolve operation    
    Then installation contains bar version 2.0-1
    And changes contains update of zonk from 2.0-1 to 2.1-1
    And suggested is empty
    And conflicts is empty     
 
  Scenario: Installation von zwei Paketen die einen Konflikt miteinander haben, Paket foo schon installiert
    Given installed package foo in version 2.0-1
    When start new operation
    And install package foo2 version 2.0-1
    Then resolve operation    
    And changes is empty
    And suggested is empty
    And conflicts contains foo2 2.0-1 to foo 2.0-1
 
  Scenario: Installation von zwei Paketen die einen Konflikt miteinander haben, in einer Transaktion
    When start new operation
    And install package foo version 2.0-1
    And install package foo2 version 2.0-1
    Then resolve operation    
    And changes is empty
    And suggested is empty
    And conflicts contains foo 2.0-1 to foo2 2.0-1
    And conflicts contains foo2 2.0-1 to foo 2.0-1 
    
  Scenario: Installation von einem Paket welches ein Paket benötigt, welches wiederum das erste Paket benötigt, circular dependency
    When start new operation
    And install package bas2 version 2.0-1
    And install package zonk2 version 2.0-1
    Then resolve operation    
    And changes is empty
    And suggested is empty
    # Note: with current implementation, there are NO conflicts if both packages will be installed in ONE transaction
    # And conflicts contains bas2 2.0-1 to zonk2 2.0-1
    # And conflicts contains zonk2 2.0-1 to bas2 2.0-1   
    
  Scenario: Installation von drei Paketen: foo-fork, zonk, bar. bar braucht foo-fork, aber foo-fork hat einen Konflikt mit zonk.
    When start new operation
    And install package foo-fork version 2.0-1
    And install package bar version 2.0-1
    And install package zonk version 2.0-1
    And resolve operation
    Then installation contains foo-fork version 2.0-1
    And installation contains zonk version 2.0-1    
    And changes is empty
    And suggested is empty
    And conflicts contains bar 2.0-1 to zonk 2.0-1
    
  Scenario: Installation von dem Paket bar2 und bar2-dev. bar2-dev kommt nicht mit der Version von foo klar und braucht bar2. bar2 braucht foo.
    Given installed package foo in version 2.0-1
    When start new operation
    And install package foo version 2.1-1
    And install package bar2 version 2.0-1
    And install package bar2-dev version 2.0-1
    And resolve operation
    Then installation contains bar2 version 2.0-1
    And installation contains bar2-dev version 2.0-1    
    And changes contains update of foo from 2.0-1 to 2.1-1   
    And suggested is empty
    And conflicts is empty
    And unresolved is empty
    
#  # Result is not defined: bas-dev - depends:foo, conflicts:foo, replaces: foo  
#  Scenario: Die Installation von bas-dev benötigt foo und ersetzt foo.
#    When start new operation
#    And install package foo version 2.0-1
#    And install package bas-dev version 2.0-1
#    And uninstall package foo version 2.0-1
#    And resolve operation
#    Then installation contains bas-dev version 2.0-1 
#    And installation contains foo version 2.0-1    
#    And uninstalling contains foo version 2.0-1
#    And suggested is empty
#    And conflicts is empty
#    And unresolved is empty
        
  Scenario: Installation von bar2 und zonk-dev auf einem Manager der bereits foo installiert hat. Die Installation von zonk-dev ersetzt foo und bar2 benötigt foo.
    Given installed package foo in version 2.0-1
    When start new operation
    And install package bar2 version 2.0-1
    And install package zonk-dev version 2.0-1
    And resolve operation
    Then installation contains bar2 version 2.0-1
    And conflicts contains zonk-dev 2.0-1 to foo 2.0-1
    And suggested is empty
    And unresolved is empty       
    
  Scenario: bar2 soll installiert werden und benötigt foo. Im Repository gibt es zwei Pakete die foo "providen": foo und foo-fork beide sind nicht installiert. Fall 1: der Nutzer entscheidet
    When start new operation
    And install package bar2 version 2.0-1
    And install package foo-fork version 2.0-1
    And resolve operation
    Then installation contains bar2 version 2.0-1
    And installation contains foo-fork version 2.0-1
    And suggested is empty
    And unresolved is empty             
     
  Scenario: bar2 soll installiert werden und benötigt foo. Im Repository gibt es zwei Pakete die foo "providen": foo und foo-fork beide sind nicht installiert. Fall 2: der Name entscheidet.
    When start new operation
    And install package bar2 version 2.0-1
    And install package foo version 2.0-1
    And resolve operation
    Then installation contains bar2 version 2.0-1
    And installation contains foo version 2.0-1
    And suggested is empty
    And unresolved is empty      
    
  Scenario: bar2 soll installiert werden und benötigt foo. Im Repository gibt es zwei Pakete die foo "providen": foo und foo-fork beide sind nicht installiert. Fall 3: foo-fork hat einen Konflikt mit einem bereits installierten Paket zonk
    Given installed package zonk in version 2.0-1
    When start new operation
    And install package bar2 version 2.0-1
    And install package foo-fork version 2.0-1
    And resolve operation
    Then installation contains bar2 version 2.0-1
    And installation contains foo-fork version 2.0-1
    And conflicts contains zonk 2.0-1 to bar2 2.0-1
    And conflicts contains foo-fork 2.0-1 to zonk 2.0-1
    And suggested is empty
    And unresolved is empty        
        
  Scenario: bar2 soll installiert werden und benötigt foo. Im Repository gibt es zwei Pakete die foo "providen": foo und foo-fork beide sind nicht installiert. Fall 4: foo-fork hat einen Konflikt mit einem bereits installierten Paket zonk, zonk wir deinstalliert
    Given installed package zonk in version 2.0-1
    When start new operation
    And install package bar2 version 2.0-1
    And install package foo-fork version 2.0-1
    And uninstall package zonk version 2.0-1
    And resolve operation
    Then installation contains bar2 version 2.0-1
    And installation contains foo-fork version 2.0-1
    And uninstalling contains zonk version 2.0-1
    And conflicts is empty
    And suggested is empty
    And unresolved is empty
    
  Scenario: Die installation des Paketes bar2 benötigt foo. Zonk-dev ersetzt foo und ist schon installiert.
    Given installed package zonk-dev in version 2.0-1
    When start new operation
    And install package bar2 version 2.0-1
    And resolve operation
    Then installation contains bar2 version 2.0-1
    And conflicts is empty
    And suggested is empty
    And unresolved is empty    

  Scenario: Das Paket foo3 schlägt bei der Installation vor bas zu installieren. bas ist nur für foo wichtig aber foo funktioniert auch ohne bas, bas und zonk mit installieren
    When start new operation
    And install package foo3 version 2.0-1
    And install package bas version 2.0-1
    And resolve operation
    Then installation contains bas version 2.0-1
    And installation contains foo3 version 2.0-1
    And conflicts is empty
    And suggested is empty
    And unresolved is empty    

# update/downgrade        
        
  Scenario: Update / Downgrade von einem Paket
    Given installed package foo in version 2.0-1
    When start new operation
    And install package foo version 2.1-1
    And resolve operation
    Then installation is empty
    And changes contains update of foo from 2.0-1 to 2.1-1   
    And conflicts is empty
    And suggested is empty
    And unresolved is empty
    
  Scenario: Update / Downgrade von einem Paket bas das aber einen Konflikt mit der Version von Paket foo.
    Given installed package foo in version 2.0-1
    And installed package bas in version 2.0-1
    When start new operation
    And install package foo version 2.1-1
    And install package bas version 2.1-1
    And resolve operation
    Then installation is empty
    And changes contains update of foo from 2.0-1 to 2.1-1   
    And changes contains update of bas from 2.0-1 to 2.1-1   
    And conflicts is empty
    And suggested is empty
    And unresolved is empty    
                
  Scenario: Das Update / Downgrade eines Paketes benötigt ein nicht installiertes Paket
    Given installed package zonk in version 2.0-1
    When start new operation
    And install package foo version 2.0-1
    And install package zonk version 2.1-1
    And resolve operation
    Then installation contains foo version 2.0-1
    And changes contains update of zonk from 2.0-1 to 2.1-1   
    And conflicts is empty
    And suggested is empty
    And unresolved is empty       
    
  Scenario: Ein Paket bar2 wird upgedatet und das update ersetzt das schon installierte Paket foo.
    Given installed package foo in version 2.0-1
    And installed package bar2 in version 2.0-1
    When start new operation
    And install package bar2 version 2.1-1
    And uninstall package foo version 2.0-1
    And resolve operation
    Then installation is empty
    And changes contains update of bar2 from 2.0-1 to 2.1-1
    And uninstalling contains foo version 2.0-1
    And conflicts is empty
    And suggested is empty
    And unresolved is empty       
                    
                
# TODO: Installation-Step könnte ein Set sein, um doppelte Pakete zu vermeiden    
# TODO: install und gleichzeitiges uninstall eines Pakets müssen konsistent behandelt werden
#       (uninstall berücksichtigt aktuell nur installedPackages, nicht zusätzlich die zu installierenden und ggf. die über dependencies hinzugekommenen)
#
# TODO: Wie soll 'suggests' behandelt werden? Aktuell wird es ignoriert, soll suggests wie 'depends' behandlet werden (wird mit installiert und anschließend auf Konflike geprüft, und wenn Konflikte dann entfernen) oder wie? 
# 
# Evtl. muss der 'resolve'-Prozess so geändert werden dass:
#   1. erstellen 'installableAndExistingPackages' 
#   2. Checks auf conflict und uninstall - also nochmal ein Auge auf die Reihenfolge