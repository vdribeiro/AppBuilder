package com.app.builder.core.security

import kotlin.js.Promise
import kotlinx.coroutines.await
import kotlinx.coroutines.withContext
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.platform.developmentMode
import com.app.builder.core.telemetry.Telemetry

actual suspend fun encrypt(content: String): String? = withContext(context = Dispatcher.Default) {
    runCatching {
        encryptJs(content = content).await().toString()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to encrypt", throwable = it)
    }.getOrNull() ?: if (developmentMode) return@withContext content else null
}

actual suspend fun decrypt(content: String): String? = withContext(context = Dispatcher.Default) {
    runCatching {
        decryptJs(contentBase64 = content).await().toString()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to decrypt", throwable = it)
    }.getOrNull() ?: if (developmentMode) return@withContext content else null
}

/**
 * Encrypts [content] with AES-GCM using a non-extractable key stored in IndexedDB, generating the key on first use.
 *
 * @param content The plain text to encrypt.
 * @return A [Promise] resolving to the Base64 encoded string of the IV concatenated with the ciphertext.
 */
@JsFun(
    code = """
        async (content) => {
            const dbName = "AppSecurity";
            const storeName = "keys";
            const keyId = "master_key";
            
            const getKey = () => new Promise((resolve, reject) => {
                const request = indexedDB.open(dbName, 1);
                request.onupgradeneeded = (e) => e.target.result.createObjectStore(storeName);
                request.onsuccess = (e) => {
                    const db = e.target.result;
                    const tx = db.transaction(storeName, "readonly");
                    const store = tx.objectStore(storeName);
                    const getReq = store.get(keyId);
                    
                    getReq.onsuccess = async () => {
                        if (getReq.result) {
                            resolve(getReq.result);
                        } else {
                            try {
                                const newKey = await crypto.subtle.generateKey(
                                    { name: "AES-GCM", length: 256 },
                                    false, // SECURITY: Marks key as NON-EXTRACTABLE
                                    ["encrypt", "decrypt"]
                                );
                                // New transaction needed because 'await' yields the event loop
                                const writeTx = db.transaction(storeName, "readwrite");
                                const writeReq = writeTx.objectStore(storeName).put(newKey, keyId);
                                writeReq.onsuccess = () => resolve(newKey);
                                writeReq.onerror = () => reject(writeReq.error);
                            } catch (err) {
                                reject(err);
                            }
                        }
                    };
                    getReq.onerror = () => reject(getReq.error);
                };
                request.onerror = () => reject(request.error);
            });
    
            const key = await getKey();
            const encoder = new TextEncoder();
            const data = encoder.encode(content);
            const iv = crypto.getRandomValues(new Uint8Array(12));
            const encrypted = await crypto.subtle.encrypt({ name: "AES-GCM", iv: iv }, key, data);
            
            const combined = new Uint8Array(12 + encrypted.byteLength);
            combined.set(iv, 0);
            combined.set(new Uint8Array(encrypted), 12);
            
            // Base64 encode without causing stack overflows on large strings
            let binary = '';
            for (let i = 0; i < combined.byteLength; i++) {
                binary += String.fromCharCode(combined[i]);
            }
            return btoa(binary);
        }
        """
)
private external fun encryptJs(content: String): Promise<JsString>

/**
 * Decrypts a Base64 encoded [contentBase64] string previously produced by [encryptJs], using the key stored in IndexedDB.
 *
 * @param contentBase64 The Base64 encoded IV-plus-ciphertext string to decrypt.
 * @return A [Promise] resolving to the decrypted plain text.
 */
@JsFun(
    code = """
        async (contentBase64) => {
            const dbName = "AppSecurity";
            const storeName = "keys";
            const keyId = "master_key";
            
            const getKey = () => new Promise((resolve, reject) => {
                const request = indexedDB.open(dbName, 1);
                request.onupgradeneeded = (e) => e.target.result.createObjectStore(storeName);
                request.onsuccess = (e) => {
                    const db = e.target.result;
                    const tx = db.transaction(storeName, "readonly");
                    const getReq = tx.objectStore(storeName).get(keyId);
                    getReq.onsuccess = () => {
                        if (getReq.result) resolve(getReq.result);
                        else reject(new Error("Master key not found in IndexedDB"));
                    };
                    getReq.onerror = () => reject(getReq.error);
                };
                request.onerror = () => reject(request.error);
            });
    
            const key = await getKey();
            const binaryString = atob(contentBase64);
            const combined = new Uint8Array(binaryString.length);
            for (let i = 0; i < binaryString.length; i++) {
                combined[i] = binaryString.charCodeAt(i);
            }
            
            const iv = combined.slice(0, 12);
            const data = combined.slice(12);
            const decrypted = await crypto.subtle.decrypt({ name: "AES-GCM", iv: iv }, key, data);
            
            const decoder = new TextDecoder();
            return decoder.decode(decrypted);
        }
        """
)
private external fun decryptJs(contentBase64: String): Promise<JsString>

actual suspend fun hash(content: String): String? = withContext(context = Dispatcher.Default) {
    if (developmentMode) return@withContext content
    runCatching {
        hashJs(content = content).await().toString()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to hash", throwable = it)
    }.getOrNull() ?: if (developmentMode) return@withContext content else null
}

/**
 * Computes the SHA-256 hash of [content] using the Web Crypto API.
 *
 * @param content The data to hash.
 * @return A [Promise] resolving to the lowercase hexadecimal hash string.
 */
@JsFun(
    code = """
        async (content) => {
            const encoder = new TextEncoder();
            const data = encoder.encode(content);
            const hashBuffer = await crypto.subtle.digest('SHA-256', data);
            const hashArray = Array.from(new Uint8Array(hashBuffer));
            return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
        }
        """
)
private external fun hashJs(content: String): Promise<JsString>

private const val TAG = "Cryptography"