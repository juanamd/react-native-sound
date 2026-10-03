// $FlowFixMe
// $FlowFixMe
import { TurboModuleRegistry } from "react-native";

/**
 * Codegen spec for the Android RNSound TurboModule.
 *
 * Notes:
 * - Every `number` here becomes a Java `double`, so the player key arrives as a double.
 * - Callbacks (setErrorCallback / setOnCompletionListener) can only be invoked once per registration.
 * - onAudioFocusChange is a codegen event emitter: native emits it with emitOnAudioFocusChange().
 */

export default TurboModuleRegistry.getEnforcing("RNSound");