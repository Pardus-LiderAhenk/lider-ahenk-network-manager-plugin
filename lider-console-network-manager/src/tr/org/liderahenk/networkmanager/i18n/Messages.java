package tr.org.liderahenk.networkmanager.i18n;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	
	private static final String BUNDLE_NAME = "tr.org.liderahenk.networkmanager.i18n.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	public Messages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
	
}
