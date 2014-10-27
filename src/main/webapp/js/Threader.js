Tapestry.ajaxFailureHandler = function (response) {
		// 	my ajax error handler! i like it
        var content = response.responseText.stripScripts();
        var start = content.indexOf('<modal>');
        var end = content.indexOf('</modal>') + 8;
        
        content = content.substr(start,end);
        
        // check for a previous written exception modal
        var previousModal = T5.dom.locate('exceptionmodal');
        if (previousModal) {
        	T5.dom.remove(previousModal);
        }
        
        T5.dom.appendMarkup(document.body, content);
        
        $T5_JQUERY('#exceptionmodal').modal({
			  keyboard: false
			});
};


/**
 * Used by other initializers to connect an element (either a link
 * or a form) to a zone.
 *
 * @param eventName
 *            the event on the element to observe
 * @param element
 *            the element to observe for events
 * @param zoneId
 *            identified a Zone by its clientId. Alternately, the
 *            special value '^' indicates that the Zone is a
 *            container of the element (the first container with the
 *            't-zone' CSS class).
 * @param url
 *            The request URL to be triggered when the event is
 *            observed. Ultimately, a partial page update JSON
 *            response will be passed to the Zone's ZoneManager.
 */
Tapestry.Initializer.updateZoneOnEvent = function (eventName, element, zoneId, url) {
	
    element = $(element);
    
    if (element == null) {
    	// element not dom anymore, could be overwritten by other thread/ajax call, skip this.
    	return;
    }

    $T(element).zoneUpdater = true;

    var zoneElement = zoneId == '^' ? $(element).up('.t-zone')
        : $(zoneId);

    if (!zoneElement) {
        Tapestry
            .error(
            "Could not find zone element '#{zoneId}' to update on #{eventName} of element '#{elementId}'.",
            {
                zoneId: zoneId,
                eventName: eventName,
                elementId: element.id
            });
        return;
    }

    /*
     * Update the element with the id of zone div. This may be
     * changed dynamically on the client side.
     */

    $T(element).zoneId = zoneElement.id;

    if (element.tagName == "FORM") {

        // Create the FEM if necessary.
        element.addClassName(Tapestry.PREVENT_SUBMISSION);

        /*
         * After the form is validated and prepared, this code will
         * process the form submission via an Ajax call. The
         * original submit event will have been cancelled.
         */

        element
            .observe(
            Tapestry.FORM_PROCESS_SUBMIT_EVENT,
            function () {
                var zoneManager = Tapestry
                    .findZoneManager(element);

                if (!zoneManager)
                    return;

                var successHandler = function (transport) {
                    zoneManager
                        .processReply(transport.responseJSON);
                };

                element.sendAjaxRequest(url, {
                    parameters: {
                        "t:zoneid": zoneId
                    },
                    onSuccess: successHandler
                });
            });

        return;
    }

    /* Otherwise, assume it's just an ordinary link or input field. */

    element.observeAction(eventName, function (event) {
        element.fire(Tapestry.TRIGGER_ZONE_UPDATE_EVENT);
    });

    element.observe(Tapestry.TRIGGER_ZONE_UPDATE_EVENT, function () {

        var zoneObject = Tapestry.findZoneManager(element);

        if (!zoneObject)
            return;

        /*
         * A hack related to allowing a Select to perform an Ajax
         * update of the page.
         */

        var parameters = {};

        if (element.tagName == "SELECT" && element.value) {
            parameters["t:selectvalue"] = element.value;
        }

        zoneObject.updateFromURL(url, parameters);
    });
};


function selectFolder(id) {
	// first remove old selected folder
	var f = $('folderzone').select('.selected')[0]; //#folder'+name).innerHTML='<b>'+name+'</b>';
	f.removeClassName('selected');
	f.addClassName('notselected');
	$('folderzone').select('#'+id)[0].addClassName('selected');
}

