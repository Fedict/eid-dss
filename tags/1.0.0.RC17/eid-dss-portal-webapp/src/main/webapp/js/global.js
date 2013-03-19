function popitup(url) {
	newwindow = window.open(url, '', 'height=315,width=755');
	if (window.focus) {
		newwindow.focus();
	}
	return false;
}