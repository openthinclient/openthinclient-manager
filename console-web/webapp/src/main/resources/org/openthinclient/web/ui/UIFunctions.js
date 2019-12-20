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

function addMACAdressHandler() {
  document.querySelectorAll('input.key-macaddress').forEach(mac => {
      mac.onkeyup = ev => {
        if(ev.key == 'Enter' && mac.value.indexOf(':') == -1 && mac.value.length == 12) {
          mac.value = mac.value.split(/(..)/).filter(Boolean).join(':')
        }
      }
  })
}
