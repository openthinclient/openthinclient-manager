function installGridTooltips() {
  document.documentElement.onmouseover = ({target}) => {
    if(target.matches('.v-grid-cell, .referenceItem .v-label')
        && target.scrollWidth > target.clientWidth) {
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
