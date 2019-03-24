#ifndef SRC_TURNSTILE_H_
#define SRC_TURNSTILE_H_

#include <condition_variable>
#include <memory>
#include <queue>
#include <type_traits>

class Turnstile {
public:
    std::mutex mtex;
    std::condition_variable cv;
    size_t how_many_waiting;
    bool ready_for_next;

    Turnstile();
};

class Turnstile_Pool {
private:
    size_t INIT_T;
    std::queue<Turnstile *> available_turnstiles;
    std::mutex shield;

public:
    Turnstile_Pool();

    ~Turnstile_Pool();

    Turnstile *give_turnstile();

    void get_turnstile_back(Turnstile *turnstile);

private:
    void shrink();

    void expand();
};

class Mutex {
public:
    Turnstile *ptr;

    Mutex();

    Mutex(const Mutex &) = delete;

    void lock();    // NOLINT

    void unlock();  // NOLINT
};

#endif  // SRC_TURNSTILE_H_
