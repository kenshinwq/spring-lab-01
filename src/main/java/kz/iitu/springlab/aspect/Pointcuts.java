package kz.iitu.springlab.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class Pointcuts {

    // Срез для всех классов внутри пакета service и его подпакетов
    @Pointcut("within(kz.iitu.springlab.service..*)")
    public void serviceLayer() { }

    // Срез для всех публичных методов
    @Pointcut("execution(public * *(..))")
    public void publicMethod() { }

    // Комбинированный срез: публичные методы сервисного слоя
    @Pointcut("serviceLayer() && publicMethod()")
    public void serviceOperation() { }
}