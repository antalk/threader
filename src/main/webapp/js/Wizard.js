function progressBar() {
  	$T5_JQUERY('#rootwizard').bootstrapWizard({onTabShow: function(tab, navigation, index) {
		var $total = navigation.find('li').length;
		var $current = index+1;
		var $percent = ($current/$total) * 100;
		$T5_JQUERY('#rootwizard').find('.progress-bar').css({width:$percent+'%'});
		
		if($current == $total) {
			$T5_JQUERY('#rootwizard').find('#submits').show();
		} else {
			$T5_JQUERY('#rootwizard').find('#submits').hide();
		}
	}});
};