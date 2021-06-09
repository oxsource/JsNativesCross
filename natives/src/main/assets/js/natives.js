(function () {
    function jsonString(obj) {
        return typeof (obj) === 'string' ? obj : JSON.stringify(obj);
    }

    function jsonObject(obj) {
        if (typeof (obj) !== 'string') return obj;
        if (!obj.startsWith("{")) return obj;
        try {
            return JSON.parse(obj);
        } catch (e) {
            return obj;
        }
    }

    const ERROR_PREFIX = 'ERROR@';
    const JS2NATIVE_CALLBACK = 'JS2NATIVE_CALLBACK'
    const JS_CALLBACKS = [];
    const JS_MODULES = {};
    let lastInvokeStamp = 0;

    /**
     * JS端调用原生方法后的回调分发
     */
    function handleJs2NativeCallback(method, params) {
        const saved = JS_CALLBACKS.find(e => e.id == method)
        if (!saved || typeof (saved.resolve) != 'function' || typeof (saved.reject) != 'function') {
            return `${ERROR_PREFIX}callback id(${method}) not exist.`
        }
        let value = ''
        try {
            if (params.startsWith(ERROR_PREFIX)) {
                saved.reject(params.replace(ERROR_PREFIX, ''))
            } else {
                saved.resolve(jsonObject(params))
            }
        } catch (e) {
            value = `${ERROR_PREFIX}${e}.`
        } finally {
            JS_CALLBACKS.pop(saved)
        }
        return value
    }

    /**
     * 原生调用JS端的方法(window._native2js)
     */
    window._native2js = function (path, payload) {
        const paths = path.split('/');
        if (!paths || paths.length != 2) {
            return `${ERROR_PREFIX}path mismatch.`
        }
        const moduleKey = paths[0];
        const methodKey = paths[1];
        //invoke js callback
        if (JS2NATIVE_CALLBACK == moduleKey) {
            return handleJs2NativeCallback(methodKey, payload)
        }
        //invoke js function
        const module = JS_MODULES[moduleKey]
        if (!module) {
            return `${ERROR_PREFIX}NoSuchJSModule(${moduleKey}).`
        }
        const caller = module[methodKey];
        if (typeof (caller) !== 'function') {
            return `${ERROR_PREFIX}NoSuchJSMethod(${methodKey}).`
        }
        return caller(jsonObject(payload));
    }

    window._natives = {
        /**
         * JS端添加供原生调用的模块
         */
        inject: function (modules) {
            if (!modules || typeof (modules) != 'object') return
            Object.assign(JS_MODULES, modules)
        },

        /**
         * JS端移除供原生调用的模块
         */
        eject: function (modules) {
            if (!modules || typeof (modules) != 'object') return
            Object.keys(modules).forEach((key) => delete JS_MODULES[key])
        },

        /**
         * JS端调用原生的方法
         */
        invoke: function (path, payload) {
            return new Promise(function (resolve, reject) {
                const js2native = (function () {
                    const android = window
                    if (android._js2native) {
                        return function (path, payload, callback) {
                            android._js2native.invoke(path, payload, callback)
                        }
                    }
                    const ios = (window.webkit || {}).messageHandlers || {}
                    if (ios._js2native) {
                        return function (path, payload, callback) {
                            ios._js2native.postMessage({ path, payload, callback })
                        }
                    }
                    return null
                })()
                if (!js2native) return reject('js2native is undefined.');
                const json = jsonString(payload);
                let stamp = new Date().getTime()
                stamp = stamp == lastInvokeStamp ? stamp + 1 : stamp
                lastInvokeStamp = stamp
                const id = `${stamp}`
                JS_CALLBACKS.push({ id, resolve, reject })
                js2native(path, json, id);
            })
        }
    };
})();