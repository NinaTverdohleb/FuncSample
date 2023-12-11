package com.example.myapplication.sample

// работа с данными
interface UserDataSource {
    suspend fun getUsersByName(name: String): List<User>
    suspend fun getProfile(id: String): Profile
    suspend fun getUserFriends(id: String): List<User>
    suspend fun getCurrentUser(): User
    suspend fun addFriendForCurrent(userid: String, friendId: String)
}

data class Profile(
    val id: String,
    val name: String,
    val friendsCount: Int,
    val isStudent: Boolean
)

data class User(
    val id: String,
    val name: String,
)

class MapperUserLogic {
    fun mapToUser(profile: Profile) = UserLogic(profile.id, profile.name, profile.friendsCount)
}

class UserRepositoryImpl(
    private val userDataSource: UserDataSource,
    private val mapper: MapperUserLogic
) : UserRepository {
    override suspend fun getCurrentUserId(): String {
        val user = userDataSource.getCurrentUser()
        return user.id
    }

    override suspend fun getUser(id: String): UserLogic {
        val profile = userDataSource.getProfile(id)
        return mapper.mapToUser(profile)
    }

    override suspend fun findUsersByName(name: String): List<UserLogic> {
        val users = userDataSource.getUsersByName(name)
        val usersDto = mutableListOf<UserLogic>()
        for (i in users.indices) {
            val profile = userDataSource.getProfile(users[i].id)
            usersDto.add(mapper.mapToUser(profile))
        }
        return usersDto
    }

    override suspend fun addFriend(userId: String, friendId: String) {
        userDataSource.addFriendForCurrent(userId, friendId)
    }

    override suspend fun getUserFriends(id: String): List<UserLogic> {
        val friends = userDataSource.getUserFriends(id)
        val usersDto = mutableListOf<UserLogic>()
        for (i in friends.indices) {
            val profile = userDataSource.getProfile(friends[i].id)
            usersDto.add(mapper.mapToUser(profile))
        }
        return usersDto
    }
}


// слой бизнесс логики

data class UserLogic(
    val id: String,
    val name: String,
    val friendsCount: Int
)

interface UserRepository {
    suspend fun getCurrentUserId(): String
    suspend fun getUser(id: String): UserLogic
    suspend fun findUsersByName(name: String): List<UserLogic>
    suspend fun addFriend(userId: String, friendId: String)
    suspend fun getUserFriends(id: String): List<UserLogic>
}

class UserInteractor(
    private val repository: UserRepository
) {
    private var currentUser: UserLogic? = null

    suspend fun getCurrentUser(): UserLogic {
        if (currentUser == null) {
            val currentUserId = repository.getCurrentUserId()
            currentUser = getUser(currentUserId)
        }
        return currentUser!!
    }

    suspend fun getUser(userId: String): UserLogic = repository.getUser(userId)

    suspend fun getMyFriends(): List<UserLogic> {
        return getUserFriends(getCurrentUser().id)
    }

    suspend fun getUserFriends(id: String): List<UserLogic> = repository.getUserFriends(id)

    suspend fun addToMeAllFriendsWithName(name: String) {
        val friends = repository.findUsersByName(name)
        for (i in friends.indices) {
            repository.addFriend(getCurrentUser().id, friends[i].id)
        }
    }
}

// место вызова интерактора
suspend fun sample(userInteractor: UserInteractor) {
    try {
        val user = userInteractor.getCurrentUser() // любой метод интерактора
    } catch(e: Exception) {
        // действие соответствующее ошибки
        // здесь у нас может быть вызов подготовленного обработчика ошибок
        // который по типу исключения сделает вывод о дальнейших действиях
    }
}
