package com.zmxv.RNSound;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.BaseReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;

import java.util.HashMap;
import java.util.Map;

public class RNSoundPackage extends BaseReactPackage {

	@Nullable
	@Override
	public NativeModule getModule(@NonNull String name, @NonNull ReactApplicationContext context) {
		if (name.equals(RNSoundModule.NAME)) {
			return new RNSoundModule(context);
		}
		return null;
	}

	@NonNull
	@Override
	public ReactModuleInfoProvider getReactModuleInfoProvider() {
		return new ReactModuleInfoProvider() {
			@NonNull
			@Override
			public Map<String, ReactModuleInfo> getReactModuleInfos() {
				final Map<String, ReactModuleInfo> moduleInfos = new HashMap<>();
				moduleInfos.put(RNSoundModule.NAME, new ReactModuleInfo(
					RNSoundModule.NAME,
					RNSoundModule.NAME,
					false, // canOverrideExistingModule
					false, // needsEagerInit
					false, // isCxxModule
					true // isTurboModule
				));
				return moduleInfos;
			}
		};
	}

}
