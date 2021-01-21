package com.reactlibrary.Impl;


import com.reactlibrary.OuiStepReceiver;

public class OuiStepReceiverImpl extends OuiStepReceiver {
	@Override
	public Class getServiceClass() {
		return OuiStepServiceImpl.class;
	}
}
