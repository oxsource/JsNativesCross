 (function () {
     function jsonString(obj) {
         return typeof (obj) === 'string' ? obj : JSON.stringify(obj)
     }

     function jsonObject(obj) {
         if (typeof (obj) !== 'string') return obj
         if (!obj.startsWith("{")) return obj
         try {
             return JSON.parse(obj)
         } catch (e) {
             return obj
         }
     }

     function response(success, msg) {
         return { success: success, msg: msg }
     }

     const JS2NATIVE_CALLBACK = 'JS2NATIVE_CALLBACK'
     const JS_CALLBACKS = []
     const JS_MODULES = {}
     let lastInvokeStamp = 0
     let js2native = null

     /**
      * JS端调用原生方法后的回调分发
      */
     function handleJs2NativeCallback(method, params) {
         const saved = JS_CALLBACKS.find(e => e.id == method)
         if (!saved || typeof (saved.resolve) != 'function') {
             return response(false, `callback id(${method}) not exist.`)
         }
         let value = ''
         try {
             saved.resolve(jsonObject(params))
         } catch (e) {
             value = `${e}.`
             saved.resolve(response(false, value))
         } finally {
             JS_CALLBACKS.pop(saved)
         }
         return response(value.length <= 0, value)
     }

     /**
      * 原生调用JS端的方法(window._native2js)
      */
     window._native2js = function (params) {
         //compat android and ios
         params = jsonObject(params)
         if (!params || typeof (params) != 'object') {
             return response(false, 'native2js params type is not object.')
         }
         const path = params.path || ""
         delete params.path
         const paths = path.split('/')
         if (!paths || paths.length != 2) {
             return response(false, 'path mismatch.')
         }
         const payload = params.payload || {}
         const moduleKey = paths[0]
         const methodKey = paths[1]
         //invoke js callback
         if (JS2NATIVE_CALLBACK == moduleKey) {
             return handleJs2NativeCallback(methodKey, payload)
         }
         //invoke js function
         const module = JS_MODULES[moduleKey]
         if (!module) {
             return response(false, `NoSuchJSModule(${moduleKey}).`)
         }
         const caller = module[methodKey]
         if (typeof (caller) !== 'function') {
             return response(false, `NoSuchJSMethod(${methodKey}).`)
         }
         return caller(jsonObject(payload))
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
                 const invoker = js2native || (() => {
                     const android = window._js2native
                     if (android) return (path, args, cb) => android.invoke(path, args, cb)
                     const ios = ((window.webkit || {}).messageHandlers || {})._js2native
                     if (ios) return (path, args, cb) => ios.postMessage({ path, args, cb })
                     return null
                 })()
                 js2native = invoker
                 if (!invoker) return reject(response(false, 'js2native is undefined.'))
                 const json = jsonString(payload)
                 let stamp = new Date().getTime()
                 stamp = stamp == lastInvokeStamp ? stamp + 1 : stamp
                 lastInvokeStamp = stamp
                 const id = `${stamp}`
                 JS_CALLBACKS.push({ id, resolve, reject })
                 invoker(path, json, `${JS2NATIVE_CALLBACK}/${id}`)
             })
         }
     }
 })()
