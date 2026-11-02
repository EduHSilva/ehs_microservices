package com.edu.silva.study.infra.exceptions

class EntityNotFoundException(entityName: String, id: Any?) :
    RuntimeException("$entityName não encontrado: $id")

class InvalidRequestException(message: String) : RuntimeException(message)
