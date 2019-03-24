#include "turnstile.h"

#include <cassert>
#include <vector>

static const size_t GUARDIANS = 271;
std::vector<std::mutex> &guardians() {
    static std::vector<std::mutex> guardians(GUARDIANS);
    return guardians;
}

char dummy = 'a';
static Turnstile *first_is_in = reinterpret_cast<Turnstile *>(dummy);

Turnstile_Pool &turnstile_pool() {
    static Turnstile_Pool tp;
    return tp;
}

Turnstile::Turnstile() {
    how_many_waiting = 0;
    ready_for_next = false;
}

Turnstile_Pool::Turnstile_Pool() {
    std::unique_lock<std::mutex> lk(shield);
    INIT_T = 4;

    for (size_t i = 0; i < INIT_T; ++i) {
        available_turnstiles.push(new Turnstile);
    }
}

Turnstile_Pool::~Turnstile_Pool() {
    while (!available_turnstiles.empty()) {
        auto temp = available_turnstiles.front();
        available_turnstiles.pop();
        delete temp;
    }
}

Turnstile *Turnstile_Pool::give_turnstile() {
    std::unique_lock<std::mutex> lk(shield);

    Turnstile *temp = available_turnstiles.front();
    available_turnstiles.pop();
    if (available_turnstiles.empty()) {
        expand();
    }

    return temp;
}

void Turnstile_Pool::expand() {
    for (size_t i = 0; i < INIT_T; ++i) {
        available_turnstiles.push(new Turnstile);
    }

    INIT_T *= 2;
}

void Turnstile_Pool::get_turnstile_back(Turnstile *turnstile) {
    std::unique_lock<std::mutex> lk(shield);

    available_turnstiles.push(turnstile);
    if (available_turnstiles.size() >= 0.75 * INIT_T) {
        shrink();
    }
}

void Turnstile_Pool::shrink() {
    for (size_t i = 0; i < INIT_T / 2; ++i) {
        auto temp = available_turnstiles.front();
        available_turnstiles.pop();
        delete temp;
    }
    INIT_T /= 2;
}

Mutex::Mutex() {
    ptr = nullptr;
    assert(sizeof(this) <= 8);
}

void Mutex::lock() {
    auto hash_from_ptr = std::hash < Mutex * > {}(this) % GUARDIANS;
    std::unique_lock<std::mutex> guard(guardians()[hash_from_ptr]);

    if (ptr == nullptr) {
        this->ptr = first_is_in;
    } else {
        if (ptr == first_is_in) {
            ptr = turnstile_pool().give_turnstile();
        }
        assert(ptr != nullptr);

        ptr->how_many_waiting++;
        guard.unlock();
        std::unique_lock<std::mutex> lk(ptr->mtex);
        ptr->cv.wait(lk, [=] { return ptr->ready_for_next; });

        guard.lock();
        assert(ptr->ready_for_next == true);
        ptr->ready_for_next = false;
    }
}

void Mutex::unlock() {
    auto hash_from_ptr = std::hash < Mutex * > {}(this) % GUARDIANS;
    std::lock_guard<std::mutex> guard(guardians()[hash_from_ptr]);

    assert(ptr != nullptr);
    if (ptr == first_is_in) {
        ptr = nullptr;
    } else if (ptr->how_many_waiting > 0) {
        std::unique_lock<std::mutex> lk(ptr->mtex);

        ptr->ready_for_next = true;
        ptr->how_many_waiting--;
        ptr->cv.notify_one();
    } else {
        assert(ptr->how_many_waiting == 0);
        assert(ptr->ready_for_next == false);

        turnstile_pool().get_turnstile_back(ptr);
        ptr = nullptr;
    }
}
