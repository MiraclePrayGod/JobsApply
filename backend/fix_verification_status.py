"""
Script para corregir el estado de verificación de trabajadores
Establece is_verified=False para trabajadores que no tienen verification_photo_url
Ejecutar: python fix_verification_status.py
"""
from app.database import SessionLocal
from app.models.worker import Worker

def fix_verification_status():
    """Corrige el estado de verificación de trabajadores"""
    db = SessionLocal()
    
    try:
        print("🔧 Corrigiendo estado de verificación...")
        
        # Obtener todos los trabajadores que están marcados como verificados pero no tienen foto
        workers_to_fix = db.query(Worker).filter(
            Worker.is_verified == True,
            (Worker.verification_photo_url == None) | (Worker.verification_photo_url == "")
        ).all()
        
        print(f"📋 Encontrados {len(workers_to_fix)} trabajadores con verificación incorrecta")
        
        for worker in workers_to_fix:
            print(f"   - {worker.full_name} (ID: {worker.id}) - Cambiando is_verified a False")
            worker.is_verified = False
        
        db.commit()
        
        print(f"\n✅ {len(workers_to_fix)} trabajadores corregidos")
        print("   Ahora todos los trabajadores sin foto de verificación tienen is_verified=False")
        
    except Exception as e:
        print(f"❌ Error: {str(e)}")
        import traceback
        traceback.print_exc()
        db.rollback()
    finally:
        db.close()

if __name__ == "__main__":
    fix_verification_status()

