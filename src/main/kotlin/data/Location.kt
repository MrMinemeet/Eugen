package data

class Location {
    var id: Int = -1
    var name: String = ""

    override fun toString(): String {
        return "Location(id=$id, name='$name')"
    }
}