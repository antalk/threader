function closeEditModal(id) {
	
	var editZone = $(id);
	
	if (editZone) {
		var modal = editZone.up('.modal.in');
		modal.hide();
		$(editZone).hide();
	}
	//// there is (always) 1 and only 1 backdrop! 
	$T5_JQUERY('.modal-backdrop').remove();
}