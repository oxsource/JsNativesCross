(function(){
    function jsonString(obj){
        return typeof(obj) === 'string' ? obj : JSON.stringify(obj);
    }

    function jsonObject(obj){
        if(typeof(obj) !== 'string') return obj;
        if(!obj.startsWith("{")) return obj;
        try{
            return JSON.parse(obj);
        }catch(e){
            return obj;
        }
    }

    const ERROR_PREFIX = 'ERROR@';
    const CALLBACK_QUEUE_NAME = 'CallbackQueue'
    const callbackQueue = [];
    const watchEvents = {};
    let lastInvokeStamp = 0;

    function handleCallback(method, params){
        const saved = callbackQueue.find(e => e.id == method)
        if(!saved || typeof(saved.resolve) != 'function' || typeof(saved.reject) != 'function'){
            return `${ERROR_PREFIX}callback id(${method}) not exist.`
        }
        let value = ''
        try{
            if (params.startsWith(ERROR_PREFIX)){
                saved.reject(params.replace(ERROR_PREFIX, ''))
            }else{
                saved.resolve(jsonObject(params))
            }
        }catch(e){
            value = `${ERROR_PREFIX}${e}.`
        }finally{
          callbackQueue.pop(saved)
        }
        return value
    }

    window._native2js = function(path, payload){
      const paths = path.split('/');
      if(!paths || paths.length != 2) {
          return `${ERROR_PREFIX}path mismatch.`
      }
      const moduleKey = paths[0];
      const methodKey = paths[1];
      //invoke js callback
      if(CALLBACK_QUEUE_NAME == moduleKey) {
          return handleCallback(methodKey, payload)
      }
      //invoke js function
      const module = watchEvents[moduleKey]
      if(!module) {
          return `${ERROR_PREFIX}NoSuchJSModule(${moduleKey}).`
      }
      const caller = module[methodKey];
      if(typeof(caller) !== 'function') {
          return `${ERROR_PREFIX}NoSuchJSMethod(${methodKey}).`
      }
      return caller(jsonObject(payload));
    }

    window._natives = {
      on: function(apis){
        if(!apis || typeof(apis) != 'object') return
        Object.assign(watchEvents, apis)
      },

      off: function(apis){
        if(!apis || typeof(apis) != 'object') return
        Object.keys(apis).forEach((key) => delete watchEvents[key])
      },

      invoke: function(path, payload){
            return new Promise(function(resolve, reject){
                const invoker = window._js2native;
                if(!invoker) return reject('js2native is undefined.');
                const json = jsonString(payload);
                let stamp = new Date().getTime()
                stamp = stamp == lastInvokeStamp? stamp + 1 : stamp
                lastInvokeStamp = stamp
                const id = `${stamp}`
                callbackQueue.push({id, resolve, reject})
                invoker.invoke(path, json, id);
            })
       }
    };
})();