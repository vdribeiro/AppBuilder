package com.app.builder.core.security

import kotlin.js.Promise
import kotlinx.coroutines.await

internal actual suspend fun encryptContent(content: String): String = encryptJs(content = content).await().toString()

internal actual suspend fun decryptContent(content: String): String = decryptJs(contentBase64 = content).await().toString()

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

internal actual suspend fun sha256(content: String): ByteArray = sha256Js(content = content).await<JsAny>().toByteArray()

/**
 * Computes the SHA-256 hash of [content] using the Web Crypto API.
 *
 * @param content The data to hash.
 * @return A [Promise] resolving to a `Uint8Array` with the digest bytes.
 */
@JsFun(
    code = """
        async (content) => {
            const encoder = new TextEncoder();
            const data = encoder.encode(content);
            return new Uint8Array(await crypto.subtle.digest('SHA-256', data));
        }
        """
)
private external fun sha256Js(content: String): Promise<JsAny>

internal actual suspend fun pbkdf2(password: String, salt: String, rounds: Int, size: Int): ByteArray =
    pbkdf2Js(password = password, salt = salt, rounds = rounds, size = size).await<JsAny>().toByteArray()

/**
 * Derives a key from [password] with PBKDF2-HMAC-SHA256 using the Web Crypto API.
 *
 * @param password The password to derive from.
 * @param salt The per-entry salt.
 * @param rounds The number of PBKDF2 rounds.
 * @param size The size, in bits, of the derived key.
 * @return A [Promise] resolving to a `Uint8Array` with the derived key bytes.
 */
@JsFun(
    code = """
        async (password, salt, rounds, size) => {
            const encoder = new TextEncoder();
            const key = await crypto.subtle.importKey("raw", encoder.encode(password), "PBKDF2", false, ["deriveBits"]);
            const bits = await crypto.subtle.deriveBits(
                { name: "PBKDF2", salt: encoder.encode(salt), iterations: rounds, hash: "SHA-256" },
                key,
                size
            );
            return new Uint8Array(bits);
        }
        """
)
private external fun pbkdf2Js(password: String, salt: String, rounds: Int, size: Int): Promise<JsAny>

/**
 * Copies this `Uint8Array` into a [ByteArray].
 *
 * @return The bytes of this `Uint8Array`.
 */
private fun JsAny.toByteArray(): ByteArray = ByteArray(size = byteCount(bytes = this)) { byteAt(bytes = this, index = it).toByte() }

/**
 * Reads the length of a `Uint8Array`.
 *
 * @param bytes The `Uint8Array` to measure.
 * @return The number of bytes in [bytes].
 */
@JsFun(code = "(bytes) => bytes.length")
private external fun byteCount(bytes: JsAny): Int

/**
 * Reads one byte of a `Uint8Array`.
 *
 * @param bytes The `Uint8Array` to read from.
 * @param index The position of the byte.
 * @return The unsigned value of the byte at [index].
 */
@JsFun(code = "(bytes, index) => bytes[index]")
private external fun byteAt(bytes: JsAny, index: Int): Int
