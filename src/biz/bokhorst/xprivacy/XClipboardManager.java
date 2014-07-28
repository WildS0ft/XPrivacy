package biz.bokhorst.xprivacy;

import java.util.ArrayList;
import java.util.List;

public class XClipboardManager extends XHook {
	private Methods mMethod;
	private String mClassName;
	private static final String cClassName = "android.content.ClipboardManager";

	private XClipboardManager(Methods method, String restrictionName) {
		super(restrictionName, method.name().replace("Srv_", ""), method.name());
		mMethod = method;
		mClassName = "com.android.server.ClipboardService";
	}

	private XClipboardManager(Methods method, String restrictionName, String className) {
		super(restrictionName, method.name(), null);
		mMethod = method;
		mClassName = className;
	}

	private XClipboardManager(Methods method, String restrictionName, String className, int sdk) {
		super(restrictionName, method.name(), null, sdk);
		mMethod = method;
		mClassName = className;
	}

	public String getClassName() {
		return mClassName;
	}

	// @formatter:off

	// public void addPrimaryClipChangedListener(OnPrimaryClipChangedListener what)
	// public ClipData getPrimaryClip()
	// public ClipDescription getPrimaryClipDescription()
	// public CharSequence getText()
	// public boolean hasPrimaryClip()
	// public boolean hasText()
	// public void removePrimaryClipChangedListener(ClipboardManager.OnPrimaryClipChangedListener what)
	// frameworks/base/core/java/android/content/ClipboardManager.java
	// http://developer.android.com/reference/android/content/ClipboardManager.html

	// @formatter:on

	// @formatter:off
	private enum Methods {
		addPrimaryClipChangedListener,
		getPrimaryClip,
		getPrimaryClipDescription,
		getText,
		hasPrimaryClip,
		hasText,
		removePrimaryClipChangedListener,

		Srv_addPrimaryClipChangedListener,
		Srv_getPrimaryClip,
		Srv_getPrimaryClipDescription,
		Srv_hasClipboardText,
		Srv_hasPrimaryClip,
		Srv_removePrimaryClipChangedListener
	};
	// @formatter:on

	public static List<XHook> getInstances(String className) {
		List<XHook> listHook = new ArrayList<XHook>();
		if (!cClassName.equals(className)) {
			if (className == null)
				className = cClassName;

			for (Methods clip : Methods.values())
				if (clip == Methods.removePrimaryClipChangedListener)
					listHook.add(new XClipboardManager(clip, null, className, 11));
				else
					listHook.add(new XClipboardManager(clip, PrivacyManager.cClipboard, className));
		}
		return listHook;
	}

	@Override
	protected void before(XParam param) throws Throwable {
		switch (mMethod) {
		case addPrimaryClipChangedListener:
		case Srv_addPrimaryClipChangedListener:
			if (isRestricted(param))
				param.setResult(null);
			break;

		case getPrimaryClip:
		case getPrimaryClipDescription:
		case getText:
		case hasPrimaryClip:
		case hasText:
			break;

		case removePrimaryClipChangedListener:
			if (isRestricted(param, PrivacyManager.cClipboard, "addPrimaryClipChangedListener"))
				param.setResult(null);
			break;

		case Srv_removePrimaryClipChangedListener:
			if (isRestricted(param, PrivacyManager.cClipboard, "Srv_addPrimaryClipChangedListener"))
				param.setResult(null);
			break;

		case Srv_getPrimaryClip:
		case Srv_getPrimaryClipDescription:
		case Srv_hasClipboardText:
		case Srv_hasPrimaryClip:
			break;
		}

	}

	@Override
	protected void after(XParam param) throws Throwable {
		switch (mMethod) {
		case addPrimaryClipChangedListener:
		case removePrimaryClipChangedListener:
		case Srv_addPrimaryClipChangedListener:
		case Srv_removePrimaryClipChangedListener:
			break;

		case getPrimaryClip:
		case getPrimaryClipDescription:
		case getText:
		case Srv_getPrimaryClip:
		case Srv_getPrimaryClipDescription:
			if (param.getResult() != null && isRestricted(param))
				param.setResult(null);
			break;

		case hasPrimaryClip:
		case hasText:
		case Srv_hasClipboardText:
		case Srv_hasPrimaryClip:
			if (isRestricted(param))
				param.setResult(false);
			break;
		}
	}
}
