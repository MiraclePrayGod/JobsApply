"""
Script rápido para verificar que los datos del seeder se guardaron correctamente
"""
import sys
import os
if sys.platform == 'win32':
    os.system('chcp 65001 >nul 2>&1')
    sys.stdout.reconfigure(encoding='utf-8') if hasattr(sys.stdout, 'reconfigure') else None

from app.database import SessionLocal
from app.models import User, Worker, Job, UserRole

db = SessionLocal()
try:
    print("="*50)
    print("VERIFICACIÓN DE DATOS EN BD")
    print("="*50)
    
    users_count = db.query(User).count()
    workers_count = db.query(Worker).count()
    jobs_count = db.query(Job).count()
    
    print(f"\n✅ Usuarios: {users_count}")
    print(f"   - Clientes: {db.query(User).filter(User.role == UserRole.CLIENT).count()}")
    print(f"   - Trabajadores: {db.query(User).filter(User.role == UserRole.WORKER).count()}")
    print(f"   - Managers: {db.query(User).filter(User.role == UserRole.MANAGER).count()}")
    
    print(f"\n✅ Trabajadores: {workers_count}")
    
    # Verificar campos Plus
    workers_with_plus = db.query(Worker).filter(Worker.is_plus_active == True).count()
    print(f"   - Con Plus activo: {workers_with_plus}")
    
    # Mostrar detalles de un trabajador con Plus
    worker_plus = db.query(Worker).filter(Worker.is_plus_active == True).first()
    if worker_plus:
        print(f"\n   📌 Ejemplo - {worker_plus.full_name}:")
        print(f"      - Plus activo: {worker_plus.is_plus_active}")
        print(f"      - Plus expira: {worker_plus.plus_expires_at}")
    
    print(f"\n✅ Trabajos: {jobs_count}")
    
    print("\n" + "="*50)
    if users_count > 0 and workers_count > 0:
        print("✅ DATOS GUARDADOS CORRECTAMENTE")
    else:
        print("⚠️ NO HAY DATOS - Ejecuta: python seed_data.py")
    print("="*50)
    
except Exception as e:
    print(f"❌ Error: {e}")
    import traceback
    traceback.print_exc()
finally:
    db.close()

