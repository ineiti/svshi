import dataclasses


@dataclasses.dataclass
class AppState:
    INT_0: int = 0
    INT_1: int = 0
    INT_2: int = 0
    INT_3: int = 0
    FLOAT_0: float = 0.0
    FLOAT_1: float = 0.0
    FLOAT_2: float = 0.0
    FLOAT_3: float = 0.0
    BOOL_0: bool = False
    BOOL_1: bool = False
    BOOL_2: bool = False
    BOOL_3: bool = False
    STR_0: str = ""
    STR_1: str = ""
    STR_2: str = ""
    STR_3: str = ""


@dataclasses.dataclass
class PhysicalState:
 GA_0_0_2: bool
 GA_0_0_1: bool



class Binary_sensor_door_lock_door_lock_sensor():
    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_1 == __return__
        """
        return physical_state.GA_0_0_1
    

class Binary_sensor_door_lock_presence_detector():
    def is_on(self, physical_state: PhysicalState) -> bool:
        """
        pre:
        post: physical_state.GA_0_0_2 == __return__
        """
        return physical_state.GA_0_0_2
    


DOOR_LOCK_DOOR_LOCK_SENSOR = Binary_sensor_door_lock_door_lock_sensor()
DOOR_LOCK_PRESENCE_DETECTOR = Binary_sensor_door_lock_presence_detector()


def door_lock_invariant(door_lock_app_state: AppState, physical_state:
    PhysicalState) ->bool:
    return True


def door_lock_iteration(door_lock_app_state: AppState, physical_state:
    PhysicalState):
    """
pre: door_lock_invariant(door_lock_app_state, physical_state)
post: door_lock_invariant(**__return__)
"""
    if not DOOR_LOCK_PRESENCE_DETECTOR.is_on(physical_state
        ) and not DOOR_LOCK_DOOR_LOCK_SENSOR.is_on(physical_state):
        if not door_lock_app_state.BOOL_0:
            if door_lock_app_state.INT_0 > 1:
                None
                door_lock_app_state.BOOL_0 = True
            else:
                door_lock_app_state.INT_0 += 1
    else:
        door_lock_app_state.INT_0 = 0
        if door_lock_app_state.BOOL_0:
            if DOOR_LOCK_PRESENCE_DETECTOR.is_on(physical_state):
                None
            elif DOOR_LOCK_DOOR_LOCK_SENSOR.is_on(physical_state):
                None
            door_lock_app_state.BOOL_0 = False
    return {'door_lock_app_state': door_lock_app_state, 'physical_state':
        physical_state}