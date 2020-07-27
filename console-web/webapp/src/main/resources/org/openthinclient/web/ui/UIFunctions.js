function installInfoButtonFunction() {
  document.documentElement.addEventListener('click', ({target}) => {
    if(target.classList.contains('context-info-button')) {
      let button = target
      let label = target.nextElementSibling;
      if(button.classList.contains('active')) {
        button.classList.remove('active')
      } else {
        button.classList.add('active')
        label.setAttribute('tabindex', -1)
        label.focus()
      }
    }
  })

  document.documentElement.addEventListener('blur', ({target, relatedTarget}) => {
    if(!relatedTarget) {
      if(!target.matches('.context-info-label, .context-info-label *')) return
      while(target && !target.classList.contains('context-info-label')) {
        target = target.parentNode
      }
    } else if((relatedTarget.matches('.context-info-button.active'))
              || !target.classList.contains('context-info-label')
              || target.contains(relatedTarget) ) {
      return
    }
    target.previousElementSibling.classList.remove('active')
  }, true)

  document.documentElement.addEventListener('keydown', ({target, code}) => {
    if(code == 'Escape') {
      if(target.classList.contains('context-info-label')) {
        target = target.previousElementSibling
      } else if(!target.matches('.context-info-button.active')) {
        return
      }
    }
    target.classList.remove('active')
  })
}

function installGridTooltips() {
  document.documentElement.onmouseover = ({target}) => {
    if( ( target.matches('.v-grid-cell, .referenceItem .v-label, .v-filterselect-suggestmenu td span')
          && target.scrollWidth > target.clientWidth )
      ||( target.matches('.overviewPanel .table .v-button')
          && [...target.querySelectorAll('.v-button-caption > *')].some(n => n.scrollWidth > n.clientWidth))) {
      target.title=target.textContent
    }
  }
}

function disableSpellcheck() {
  document.querySelectorAll('input, textarea').forEach(node => {
    node.setAttribute('spellcheck', 'false')
    node.setAttribute('autocapitalize', 'off')
    node.setAttribute('autocorrect', 'off')
    node.setAttribute('autocomplete', 'off')
  })
}

function loadBrowserFrame(browserSelector, url) {
  document.querySelectorAll(browserSelector).forEach(browserFrame => {
    if(!browserFrame.classList.contains('loading')) {
      browserFrame.classList.add('loading')
      let iframe = browserFrame.querySelector('iframe')
      iframe.addEventListener('load', () => browserFrame.classList.remove('loading'))
      iframe.src = url
    }
  })
}
