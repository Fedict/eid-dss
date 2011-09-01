function detectPlugins() {

	if (navigator.plugins && navigator.plugins.length > 0) {

		var arrayLength = navigator.plugins.length;
		var plugins = "";
		for (i = 0; i < arrayLength; i++) {
			plugins += navigator.plugins[i].name + '|';
		}
		return plugins;
	}

	return null;
}
function detectMimeTypes() {

	if (navigator.mimeTypes && navigator.mimeTypes.length > 0) {

		var arrayLength = navigator.mimeTypes.length;
		var mimeTypes = "";
		for (i = 0; i < arrayLength; i++) {
			if (null != navigator.mimeTypes[i].enabledPlugin) {
				mimeTypes += navigator.mimeTypes[i].type + ','
						+ navigator.mimeTypes[i].enabledPlugin.name + '|';
			}
		}
		return mimeTypes;
	}

	return null;
}