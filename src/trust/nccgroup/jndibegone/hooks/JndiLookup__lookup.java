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
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationDescription;
import java.lang.annotation.ElementType;
import java.util.Set;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.field.FieldDescription;
import java.util.HashMap;

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

  private static final String TARGET = "org.apache.logging.log4j.core.lookup.JndiLookup";
  private static final String SUFFIX_TARGET = ".org.apache.logging.log4j.core.lookup.JndiLookup";

  public static void hook(Instrumentation inst) {
    new AgentBuilder.Default()
    .disableClassFormatChanges()
    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
    .ignore(ElementMatchers.none())
    .type(new ElementMatcher<TypeDescription>() {
      public boolean matches(TypeDescription target) {
        if (target != null) {
          String cn = target.getCanonicalName();
          if (cn != null) {
            if (cn.endsWith(TARGET)) {
              return TARGET.equals(cn) || cn.endsWith(SUFFIX_TARGET);
            }
          }
        }
        return false;
      }
    })
    .transform(new AgentBuilder.Transformer() {
      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription td, ClassLoader cl, JavaModule mod) {
        return builder.visit(JndiLookup__lookup.getVisitor());
      }
    })
    .installOn(inst);
  }


  // we assume that the entire "org.apache.logging.log4j.core" package prefix
  // could be swapped out because that is the max length prefix for
  // https://github.com/apache/logging-log4j2 from 2.0 to 2.16.0
  // (though we only really care about 2.0-2.15.0)
  private static final String LCD_TARGET = "lookup.JndiLookup";
  private static final String LCD_SUFFIX_TARGET = ".lookup.JndiLookup";

  public static void hook_deep_match(Instrumentation inst) {
    new AgentBuilder.Default()
    .disableClassFormatChanges()
    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
    .ignore(ElementMatchers.none())
    .type(new ElementMatcher<TypeDescription>() {
      public boolean matches(TypeDescription target) {
        if (target != null) {
          String cn = target.getCanonicalName();
          if (cn != null) {
            if (cn.endsWith(LCD_TARGET)) {
              if (LCD_TARGET.equals(cn) || cn.endsWith(LCD_SUFFIX_TARGET)) {
                try {
                  return is_deep_match(target);
                } catch (Throwable t) {
                  return false;
                }
              }
            }
          }
        }
        return false;
      }
    })
    .transform(new AgentBuilder.Transformer() {
      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription td, ClassLoader cl, JavaModule mod) {
        return builder.visit(JndiLookup__lookup.getVisitor());
      }
    })
    .installOn(inst);
  }

  // next, the @org.apache.logging.log4j.core.config.plugins.Plugin annotation
  // can be matched down to config.plugins.Plugin.

  // the base class of the JndiLookup class changes over time between
  // org.apache.logging.log4j.core.lookup.StrLookup in 2.0 and then
  // org.apache.logging.log4j.core.lookup.AbstractLookup from 2.1 on.
  // we can apply the same prefix removal, so these would be lookup.StrLookup
  // and lookup.AbstractLookup.
  // we will do more traditional in-method string operations for the prefix
  // match because this is not going to be running against every single class
  // load so simplicitly > raw performance here.
  public static boolean is_deep_match(TypeDescription target) {
    AnnotationList al = target.getDeclaredAnnotations();
    AnnotationDescription[] ar = al.toArray(new AnnotationDescription[]{});
    if (ar.length != 1) {
      return false;
    }
    AnnotationDescription an = ar[0];
    System.out.println(an);
    TypeDescription ant = an.getAnnotationType();
    System.out.println(ant);
    String antn = ant.getCanonicalName();
    if (antn == null) {
      return false;
    }
    if (!"config.plugins.Plugin".equals(antn) && !antn.endsWith(".config.plugins.Plugin")) {
      return false;
    }

    HashMap<String,MethodDescription.InDefinedShape> pm = new HashMap<String,MethodDescription.InDefinedShape>();
    for (MethodDescription.InDefinedShape md : ant.getDeclaredMethods()) {
      pm.put(md.getActualName(), md);
    }

    if (!pm.containsKey("name")) {
      return false;
    } else {
      MethodDescription.InDefinedShape md = pm.get("name");
      if (!"java.lang.String".equals(md.getReturnType().getActualName())) {
        return false;
      }
      AnnotationValue<?, ?> av = an.getValue(md);
      if (av == null || av.equals(AnnotationValue.UNDEFINED)) {
        return false;
      }
      String val = (String)av.resolve();
      if (val == null || !val.equals("jndi")) {
        return false;
      }
    }

    if (!pm.containsKey("category")) {
      return false;
    } else {
      MethodDescription.InDefinedShape md = pm.get("category");
      if (!"java.lang.String".equals(md.getReturnType().getActualName())) {
        return false;
      }
      AnnotationValue<?, ?> av = an.getValue(md);
      if (av == null || av.equals(AnnotationValue.UNDEFINED)) {
        return false;
      }
      String val = (String)av.resolve();
      if (val == null || !val.equals("Lookup")) {
        return false;
      }
    }

    if (!pm.containsKey("elementType")) {
      return false;
    } else {
      MethodDescription.InDefinedShape md = pm.get("elementType");
      if (!"java.lang.String".equals(md.getReturnType().getActualName())) {
        return false;
      }
      AnnotationValue<?, ?> av = an.getValue(md);
      if (av == null || av.equals(AnnotationValue.UNDEFINED)) {
        return false;
      }
      String val = (String)av.resolve();
      if (val == null || !val.equals("")) {
        return false;
      }
    }

    if (!pm.containsKey("printObject")) {
      return false;
    } else {
      MethodDescription.InDefinedShape md = pm.get("printObject");
      if (!"boolean".equals(md.getReturnType().getActualName())) {
        return false;
      }
      AnnotationValue<?, ?> av = an.getValue(md);
      if (av == null || av.equals(AnnotationValue.UNDEFINED)) {
        return false;
      }
      Boolean val = (Boolean)av.resolve();
      if (val == null || val != false) {
        return false;
      }
    }

    if (!pm.containsKey("deferChildren")) {
      return false;
    } else {
      MethodDescription.InDefinedShape md = pm.get("deferChildren");
      if (!"boolean".equals(md.getReturnType().getActualName())) {
        return false;
      }
      AnnotationValue<?, ?> av = an.getValue(md);
      if (av == null || av.equals(AnnotationValue.UNDEFINED)) {
        return false;
      }
      Boolean val = (Boolean)av.resolve();
      if (val == null || val != false) {
        return false;
      }
    }

    //todo: iface/subclass match


    HashMap<String,MethodDescription.InDefinedShape> tmm = new HashMap<String,MethodDescription.InDefinedShape>();
    for (MethodDescription.InDefinedShape md : target.getDeclaredMethods()) {
      tmm.put(md.getActualName(), md);
      System.out.println(md);
    }
    System.out.println("-------");
    HashMap<String,FieldDescription.InDefinedShape> tfm = new HashMap<String,FieldDescription.InDefinedShape>();
    for (FieldDescription.InDefinedShape fd : target.getDeclaredFields()) {
      tfm.put(fd.getActualName(), fd);
      System.out.println(fd);
    }

    if (!tmm.containsKey("lookup")) {
      return false;
    } else {

    }

    if (!tmm.containsKey("convertJndiName")) {
      return false;
    } else {

    }

    if (!tfm.containsKey("CONTAINER_JNDI_RESOURCE_PATH_PREFIX")) {
      return false;
    } else {

    }

    if (!tfm.containsKey("")) {
      return false;
    } else {

    }



    return true;
  }

}
