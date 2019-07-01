function installGridTooltips() {
  document.querySelector('#mainmenu').onmouseover = ({target}) => {
    if(target.classList.contains('v-grid-cell')
        && target.scrollWidth > target.clientWidth) {
      target.title=target.textContent
    }
  }
}

function disableSpellcheck() {
  document.querySelectorAll('input[type=text],textarea').forEach(node => {
    node.node.setAttribute('spellcheck', 'false')
    node.node.setAttribute('autocapitalize', 'off')
    node.node.setAttribute('autocorrect', 'off')
    node.node.setAttribute('autocomplete', 'off')
  })
}
