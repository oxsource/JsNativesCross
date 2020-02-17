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

    function jsonResult(success, data, msg){
       const obj = jsonString(data);
       const result = {success, msg, data: obj};
       return result;
    };

    window._natives = {
      on: function(apis){
          window._native2js = function(request){
              const module = apis[request.module];
              const natives = window._natives;
              if(!module) return natives.failure(msg = `NoSuchJSModule: ${request.module}`);
              const caller = module[request.method];
              if(typeof(caller) !== 'function') return natives.failure(msg = `NoSuchJSMethod: ${request.method}@${request.module}`);
              const payload = jsonObject(request.payload);
              return caller.call(caller, payload);
          }
      },

      require: function(requires, use = true){
          const natives = window._natives;
          return natives.invoke('MODULE_PROVIDER', use ? 'inject': 'reject', requires)
      },

      invoke: function(module, method, payload){
           const natives = window._natives;
           const invoker = window._js2native;
           if(!invoker) return natives.failure(msg = `js2native is undefined.`);;
           const obj = jsonString(payload);
           const requestText = JSON.stringify({module, method, payload: obj});
           const responseText = invoker.invoke(requestText);
           return JSON.parse(responseText);
       },

       success: function(data = "", msg = ""){
           return jsonResult(success = true, data = data, msg = msg);
       },

       failure: function(msg = "", data = ""){
           return jsonResult(success = false, data = data, msg = msg);
       }
    };
})();
