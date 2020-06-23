(function() {
  var ajaxCall = {
    url: '/actuator/health',
    type: 'GET',
    tryCount: 0,
    retryLimit: 200,
    success: function (json) {
      // The server seems to be available again. Redirect the user to the user interface
      window.location = '/';
    },
    error: function (xhr, textStatus, errorThrown) {
      ajaxCall.tryCount++;
      if (ajaxCall.tryCount <= this.retryLimit) {
        //try again in two seconds.
        setTimeout(function() {
          $.ajax(ajaxCall)
        }, 2000);
      } else {
        // the limit has been reached. Notify the user.
        $('.progress').hide();
        $('.description').hide();
        $('.error').show();
      }
    }
  };
  $.ajax(ajaxCall);

})();

