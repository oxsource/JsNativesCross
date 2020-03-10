(function(){
    function jsonString(obj){
        return typeof(obj) === 'string' ? obj : JSON.stringify(obj);
    };

    function jsonObject(obj){
        if(typeof(obj) !== 'string') return obj;
        if(!obj.startsWith("{")) return obj;
        try{
            return JSON.parse(obj);
        }catch(e){
            return obj;
        }
    };

    const callbackQueue = [];
    let lastInvokeStamp = 0;

    function handleCallback(method, params){
        const saved = callbackQueue.find(e => e.id == method)
        if(!saved || typeof(saved.resolve) != 'function'){
            console.log(`native2js callback id(${method}) not exist.`);
            return
        }
        try{
            saved.resolve(params)
        }catch(e){
            console.log(e);
        }finally{
          callbackQueue.pop(saved)
        }
    };

    window._natives = {
      on: function(apis){
          window._native2js = function(path, payload){
              const paths = path.split('/');
              if(!paths || paths.length != 2) {
                  console.log('native2js path mismatch.');
                  return ''
              }
              const moduleKey = paths[0];
              const methodKey = paths[1];
              const params = jsonObject(payload);
              //invoke js callback
              if('CallbackQueue' == moduleKey) {
                  return handleCallback(methodKey, params)
              }
              //invoke js function
              const module = apis[moduleKey]
              if(!module) {
                  console.log(`native2js NoSuchJSModule(${moduleKey}).`);
                  return ''
              }
              const caller = module[methodKey];
              if(typeof(caller) !== 'function') {
                  console.log(`native2js NoSuchJSMethod(${methodKey}).`);
                  return ''
              }
              return caller(params);
          }
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
                callbackQueue.push({id, resolve})
                invoker.invoke(path, json, id);
            })
       }
    };
})();
