# Butterfly
Simple way to temporary disable password authentication for ordinary Spring Boot apps.

This is my small "single class" Java Agent which disables password-based authentication, without touch application sources.
So when agent attached, you can easily authenticate in your app by using any text as 'password'.

Basically it does:

```
  return new BCryptPasswordEncoder() {
                @Override
                public boolean matches(CharSequence rawPassword, String encodedPassword) {
                    return true;
                }
            };
```
but with [Javassist](https://www.javassist.org/) and Java [Instrumentation API](https://docs.oracle.com/en/java/javase/11/docs/api/java.instrument/java/lang/instrument/Instrumentation.html).

