package be.limero.actor;

import be.limero.akka.message.BaseMessage;

import javax.script.*;
import java.util.Date;

public class NashornActor {

    static ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

    public static String listScriptEngines() {
        final ScriptEngineManager mgr = new ScriptEngineManager();
        String result = "";
        for (ScriptEngineFactory fac : mgr.getEngineFactories()) {
            result += String.format("%s (%s), %s (%s), %s \n", fac.getEngineName(),
                    fac.getEngineVersion(), fac.getLanguageName(),
                    fac.getLanguageVersion(), fac.getParameter("THREADING"));
        }
        return result;
    }

    public static void main(String args[]) {
        ThreadLocal<ScriptEngine> engineHolder;
        System.out.println(listScriptEngines());


        try {
            engine.eval("print('Hello World!');var fun1 = function(name) {\n" +
                    "    print('Hi there from Javascript, ' + name);\n" +
                    "    return \"greetings from javascript\";\n" +
                    "}; " +
                    " var filter=function(message){" +
                    "message.set('A',message.getLong('B')+1);" +
                    "return 'true';" +
                    "};");

            String myScript = "print('hello world');";
            Invocable invocable = (Invocable) engine;
            Compilable compilable = (Compilable) engine;
            CompiledScript script = compilable.compile(myScript);

            BaseMessage bm = new BaseMessage();
            bm.set("B", new Long(12));
            System.out.println(new Date().toString() + bm);
            Object res = null;
            for (int i = 1; i < 10000000; i++) {
                res = invocable.invokeFunction("filter", bm);
                bm.set("B", bm.getLong("A"));
            }
            System.out.println(new Date().toString() + bm);

            System.out.println(res);
            System.out.println(res.getClass());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
