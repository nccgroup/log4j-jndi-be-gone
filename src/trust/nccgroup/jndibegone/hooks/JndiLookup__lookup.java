/*
Copyright 2021 NCC Group

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package trust.nccgroup.jndibegone.hooks;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;

public class JndiLookup__lookup {

  private static final String TAG = "JndiLookup__lookup";

  @Advice.OnMethodEnter(inline = true, skipOn = Advice.OnNonDefaultValue.class)
  static String enter(@Advice.Argument(readOnly = true, value = 1) String key) {
    System.err.println("log4j jndi lookup attempted: (sanitized) " + key.replace("$", "%").replace("{","_").replace("}", "_"));
    return "(log4j jndi disabled)";
  }

  @Advice.OnMethodExit(inline = true, onThrowable = Exception.class)
  static void exit(@Advice.Enter String enter_return, @Advice.Return(readOnly = false) String ret) {
    ret = enter_return;
  }

  static AsmVisitorWrapper getVisitor() {
    return Advice.to(JndiLookup__lookup.class).on(ElementMatchers.named("lookup"));
  }

  public static void hook(Instrumentation inst) {
    new AgentBuilder.Default()
    .disableClassFormatChanges()
    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
    .ignore(ElementMatchers.none())
    .type(new ElementMatcher<TypeDescription>() {
      public boolean matches(TypeDescription target) {
        return "org.apache.logging.log4j.core.lookup.JndiLookup".equals(target.getCanonicalName());
      }
    })
    .transform(new AgentBuilder.Transformer() {
      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription td, ClassLoader cl, JavaModule mod) {
        return builder.visit(JndiLookup__lookup.getVisitor());
      }
    })
    .installOn(inst);
  }

}
