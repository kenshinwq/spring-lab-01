package kz.iitu.springlab.notify;

public interface Notifier {

    String send(String message);   // возвращает результат отправки

    String channel();              // идентификатор канала
}