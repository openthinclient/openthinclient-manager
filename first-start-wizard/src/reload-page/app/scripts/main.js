(function() {
  var ajaxCall = {
    url: '/health',
    type: 'GET',
    tryCount: 0,
    retryLimit: 100,
    success: function (json) {
      // The server seems to be available again. Redirect the user to the user interface
      window.location = '/';
    },
    error: function (xhr, textStatus, errorThrown) {
      this.tryCount++;
      if (this.tryCount <= this.retryLimit) {
        //try again in two seconds.
        setTimeout(function() {
          $.ajax(ajaxCall)
        }, 2000);
      }
    }
  };
  $.ajax(ajaxCall);

})();

